package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.*;

import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.com.rebolsysBackend.config.FacturacionApisPeruProperties;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.enums.*;
import pe.com.rebolsysBackend.exception.*;
import pe.com.rebolsysBackend.repository.*;
import pe.com.rebolsysBackend.service.service.*;

@Service
public class EmisionElectronicaServiceImpl implements EmisionElectronicaService {
  private final EmisionElectronicaRepository emisiones;
  private final ComprobanteRepository comprobantes;
  private final ComprobanteDetalleRepository lineas;
  private final PagoRepository pagos;
  private final ReglasPropuesta reglas;
  private final DocumentoFiscalBuilder builder;
  private final ProveedorFacturacionClient proveedor;
  private final FacturacionApisPeruProperties config;
  private final TransactionTemplate tx;
  private final EntityManager em;

  public EmisionElectronicaServiceImpl(EmisionElectronicaRepository emisiones,ComprobanteRepository comprobantes,
      ComprobanteDetalleRepository lineas,PagoRepository pagos,ReglasPropuesta reglas,DocumentoFiscalBuilder builder,
      ProveedorFacturacionClient proveedor,FacturacionApisPeruProperties config,PlatformTransactionManager manager,EntityManager em) {
    this.emisiones=emisiones; this.comprobantes=comprobantes; this.lineas=lineas; this.pagos=pagos;
    this.reglas=reglas; this.builder=builder; this.proveedor=proveedor; this.config=config;
    this.tx=new TransactionTemplate(manager); this.em=em;
  }

  @Override
  public Map<String,Object> configuracion() {
    return Map.of("proveedor","APISPERU","ambiente","BETA","habilitada",config.isHabilitada(),
        "configuracionPendiente",config.pendientes());
  }

  private void configurada() {
    if (!config.isHabilitada() || !config.pendientes().isEmpty())
      throw new ServicioExternoException(HttpStatus.SERVICE_UNAVAILABLE,
          "Configura y habilita la cuenta de facturación beta de APIsPerú; consulta /api/v1/facturacion/configuracion");
  }

  @Override
  public EmisionElectronicaDTO preparar(Long id,EmisionElectronicaRequestDTO r) {
    reglas.validar(r);
    return tx.execute(s->{
      var usuario=reglas.usuario(r.idUsuario());
      var c=comprobantes.bloquear(id).orElseThrow(()->new RecursoNoEncontradoException("El comprobante no existe"));
      em.refresh(c);
      String firma=reglas.firma(r);
      var porClave=emisiones.findByClaveOperacion(r.claveOperacion());
      if (porClave.isPresent()) {
        exigir(porClave.get().getComprobante().getIdComprobante().equals(id)
            && porClave.get().getFirmaSolicitud().equals(firma),"La clave corresponde a otra preparación fiscal");
        return respuesta(porClave.get());
      }
      exigir(emisiones.findByComprobanteIdComprobante(id).isEmpty(),"El comprobante ya tiene una emisión electrónica preparada");
      configurada();
      exigir(c.getEstadoComprobante()==EstadoComprobante.EMITIDO,"Emite primero la boleta o factura interna de la cuenta");
      String codigo=c.getTipoComprobante().getCodigo();
      exigir(Set.of("BOLETA","FACTURA").contains(codigo),"Solo se admiten boletas y facturas");
      exigir(c.getSerie().matches(("FACTURA".equals(codigo)?"F":"B")+"[A-Z0-9]{3}"),"La serie debe tener cuatro caracteres y comenzar por F o B");
      exigir(c.getNumeroComprobante()!=null && c.getNumeroComprobante().matches("[1-9][0-9]{0,7}"),"El correlativo electrónico admite hasta ocho dígitos");
      var pagado=pagos.findByComprobanteIdComprobante(id).stream().filter(p->p.getVerificado()==1)
          .map(Pago::getMonto).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add);
      var documento=builder.construir(c,lineas.findByComprobanteIdComprobanteOrderByNumeroLinea(id),c.getTotalPagar().subtract(pagado),r);
      var e=new EmisionElectronica();
      e.setComprobante(c); e.setUsuario(usuario); e.setClaveOperacion(r.claveOperacion()); e.setFirmaSolicitud(firma);
      e.setEstado(EstadoEmisionElectronica.PREPARADO); e.setAmbiente("BETA"); e.setRucEmisor(config.getRuc());
      e.setTipoDocumento("FACTURA".equals(codigo)?"01":"03"); e.setSerie(c.getSerie());
      e.setCorrelativo(Long.valueOf(c.getNumeroComprobante())); e.setFechaEmision(c.getFechaEmision());
      e.setFechaActualizacion(ahora()); e.setIntentos(0); e.setSolicitud(documento.solicitud());
      e.setSubtotal(documento.subtotal()); e.setIgv(documento.igv()); e.setTotal(documento.total());
      return respuesta(emisiones.saveAndFlush(e));
    });
  }

  @Override
  public EmisionElectronicaDTO obtener(Long id) {
    return tx.execute(s->respuesta(buscar(id,false)));
  }

  private EmisionElectronica buscar(Long id,boolean bloquear) {
    var e=(bloquear?emisiones.bloquearComprobante(id):emisiones.findByComprobanteIdComprobante(id))
        .orElseThrow(()->new RecursoNoEncontradoException("El comprobante no tiene emisión electrónica"));
    if (bloquear) em.refresh(e);
    return e;
  }

  private boolean definitiva(EmisionElectronica e) {
    return e.getEstado()==EstadoEmisionElectronica.ACEPTADO || e.getEstado()==EstadoEmisionElectronica.RECHAZADO;
  }
  private record Preparado(EmisionElectronicaDTO resumen,String solicitud,boolean ejecutar) {}

  @Override
  public EmisionElectronicaDTO enviar(Long id,OperacionUsuarioDTO r) {
    reglas.validar(r);
    var preparado=tx.execute(s->{
      reglas.usuario(r.idUsuario());
      var e=buscar(id,true);
      if (definitiva(e) || e.getEstado()==EstadoEmisionElectronica.ENVIANDO)
        return new Preparado(respuesta(e),e.getSolicitud(),false);
      exigir(e.getEstado()!=EstadoEmisionElectronica.INCIERTO,"Consulta el CDR antes de reenviar un documento con resultado incierto");
      configurada();
      exigir(e.getIntentos()<999999,"Se alcanzó el límite de intentos de envío");
      e.setEstado(EstadoEmisionElectronica.ENVIANDO); e.setIntentos(e.getIntentos()+1);
      e.setFechaActualizacion(ahora()); e.setCodigoRespuesta(null); e.setMensajeRespuesta(null);
      emisiones.saveAndFlush(e);
      return new Preparado(respuesta(e),e.getSolicitud(),true);
    });
    if (!preparado.ejecutar()) return preparado.resumen();
    var d=preparado.resumen();
    // El estado ENVIANDO se confirma antes del HTTP; no se mantienen bloqueos de Oracle durante la llamada.
    try {
      var resultado=proveedor.enviar(preparado.solicitud(),d.rucEmisor(),d.tipoDocumento(),d.serie(),d.correlativo());
      return guardarResultado(id,resultado);
    } catch (FacturacionProveedorException ex) {
      return guardarFallo(id,ex);
    }
  }

  @Override
  public EmisionElectronicaDTO consultar(Long id,OperacionUsuarioDTO r) {
    reglas.validar(r);
    var d=tx.execute(s->{ reglas.usuario(r.idUsuario()); var e=buscar(id,false);
      exigir(e.getIntentos()>0,"Envía el documento antes de consultar su CDR"); return respuesta(e); });
    if (d.estado()==EstadoEmisionElectronica.ACEPTADO || d.estado()==EstadoEmisionElectronica.RECHAZADO) return d;
    configurada();
    try { return guardarResultado(id,proveedor.consultar(d.rucEmisor(),d.tipoDocumento(),d.serie(),d.correlativo())); }
    catch (FacturacionProveedorException ex) {
      return guardarFallo(id,new FacturacionProveedorException(ex.getMessage(),true));
    }
  }

  private EmisionElectronicaDTO guardarResultado(Long id,ResultadoProveedorCpeDTO r) {
    return tx.execute(s->{
      var e=buscar(id,true);
      if (definitiva(e)) return respuesta(e);
      if (r.xml()!=null) e.setXmlFirmado(r.xml());
      if (r.hash()!=null) e.setHashCpe(r.hash());
      if (r.cdrZip()!=null) e.setCdrZip(r.cdrZip());
      e.setEstado(r.aceptado()==null?EstadoEmisionElectronica.INCIERTO
          :r.aceptado()?EstadoEmisionElectronica.ACEPTADO:EstadoEmisionElectronica.RECHAZADO);
      e.setCodigoRespuesta(r.codigo()); e.setMensajeRespuesta(r.mensaje()); e.setFechaActualizacion(ahora());
      return respuesta(emisiones.saveAndFlush(e));
    });
  }

  private EmisionElectronicaDTO guardarFallo(Long id,FacturacionProveedorException ex) {
    return tx.execute(s->{
      var e=buscar(id,true);
      if (definitiva(e)) return respuesta(e);
      e.setEstado(ex.isIncierto()?EstadoEmisionElectronica.INCIERTO:EstadoEmisionElectronica.ERROR_ENVIO);
      e.setCodigoRespuesta(ex.isIncierto()?"NO_CONFIRMADO":"ERROR_PROVEEDOR");
      e.setMensajeRespuesta(ex.getMessage()); e.setFechaActualizacion(ahora());
      return respuesta(emisiones.saveAndFlush(e));
    });
  }

  @Override
  public byte[] archivo(Long id,String tipo) {
    var e=tx.execute(s->buscar(id,false));
    if ("pdf".equals(tipo)) {
      configurada();
      try { return proveedor.pdf(e.getSolicitud(),e.getRucEmisor()); }
      catch (FacturacionProveedorException ex) { throw new ServicioExternoException(HttpStatus.BAD_GATEWAY,ex.getMessage()); }
    }
    if ("xml".equals(tipo)) {
      if (e.getXmlFirmado()==null) throw new RecursoNoEncontradoException("El XML firmado todavía no está disponible");
      return e.getXmlFirmado().getBytes(StandardCharsets.UTF_8);
    }
    if (e.getCdrZip()==null) throw new RecursoNoEncontradoException("El CDR todavía no está disponible");
    try {
      byte[] zip=Base64.getDecoder().decode(e.getCdrZip());
      if (zip.length<4 || zip[0]!='P' || zip[1]!='K') throw new IllegalArgumentException();
      return zip;
    } catch (IllegalArgumentException ex) {
      throw new ServicioExternoException(HttpStatus.BAD_GATEWAY,"El CDR recibido no contiene un ZIP válido");
    }
  }

  private EmisionElectronicaDTO respuesta(EmisionElectronica e) {
    return new EmisionElectronicaDTO(e.getIdEmision(),e.getComprobante().getIdComprobante(),e.getUsuario().getIdUsuario(),
        e.getAmbiente(),e.getEstado(),e.getRucEmisor(),e.getTipoDocumento(),e.getSerie(),e.getCorrelativo(),
        e.getFechaEmision(),e.getFechaActualizacion(),e.getIntentos(),e.getSubtotal(),e.getIgv(),e.getTotal(),
        e.getCodigoRespuesta(),e.getMensajeRespuesta(),e.getXmlFirmado()!=null,e.getCdrZip()!=null);
  }
}
