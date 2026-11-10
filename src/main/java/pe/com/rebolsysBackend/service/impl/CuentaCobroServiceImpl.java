package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.*;

import jakarta.persistence.EntityManager;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.entity.id.SerieComprobanteId;
import pe.com.rebolsysBackend.enums.EstadoComprobante;
import pe.com.rebolsysBackend.exception.*;
import pe.com.rebolsysBackend.repository.*;
import pe.com.rebolsysBackend.service.service.*;

/** Cobros compartidos; el servicio que llama bloquea primero la reserva o venta de origen. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class CuentaCobroServiceImpl implements CuentaCobroService {
  private final EntityManager em;
  private final CuentaReservaGestion gestion;
  private final ComprobanteRepository cuentas;
  private final ComprobanteDetalleRepository lineas;
  private final PagoRepository pagos;
  private final FormaPagoRepository formas;
  private final TipoMovimientoCajaRepository movimientos;
  private final FlujoCajaRepository caja;
  private final PerfilRepository clientes;
  private final TipoComprobanteRepository tipos;
  private final SerieComprobanteRepository series;
  private final ClienteService personas;
  private final ReglasPropuesta reglas;

  @Override
  public Comprobante pagar(Comprobante cuenta, PagoCuentaDTO request) {
    reglas.validar(request);
    var usuario = reglas.usuario(request.idUsuario());
    var c = bloquear(cuenta);
    var previo = pagos.findByClaveIdempotencia(request.claveOperacion());
    if (previo.isPresent()) {
      var p = previo.get();
      exigir(p.getComprobante().getIdComprobante().equals(c.getIdComprobante())
          && p.getUsuario().getIdUsuario().equals(request.idUsuario())
          && p.getTurnoCaja().getIdTurno().equals(request.idTurno())
          && p.getFormaPago().getIdFormaPago().equals(request.idFormaPago())
          && p.getMonto().compareTo(request.monto()) == 0
          && Objects.equals(p.getNumeroOperacion(), limpiar(request.numeroOperacion())),
          "La clave de pago ya corresponde a otra solicitud");
      return c;
    }
    var turno = gestion.turno(request.idTurno(), request.idUsuario());
    exigir(c.getEstadoComprobante() != EstadoComprobante.ANULADO, "La cuenta está anulada");
    exigir(request.monto().compareTo(gestion.saldo(c)) <= 0, "El pago supera el saldo pendiente de esta cuenta");
    var forma = formas.findById(request.idFormaPago())
        .orElseThrow(() -> new RecursoNoEncontradoException("La forma de pago no existe"));
    Long reserva = idReserva(c);
    String codigo = reserva == null ? "COBRO_VENTA" : "COBRO_RESERVA";
    var tipo = movimientos.findByNombreMovimiento(codigo)
        .orElseThrow(() -> new ReglaNegocioException("Falta configurar " + codigo));
    exigir(tipo.getSigno() == 1, codigo + " debe ser un ingreso");
    var p = new Pago();
    p.setComprobante(c);
    p.setUsuario(usuario);
    p.setTurnoCaja(turno);
    p.setFormaPago(forma);
    p.setMonto(request.monto());
    p.setNumeroOperacion(limpiar(request.numeroOperacion()));
    p.setClaveIdempotencia(request.claveOperacion());
    p.setVerificado(1);
    p.setFechaPago(ahora());
    pagos.saveAndFlush(p);
    var f = new FlujoCaja();
    f.setTurnoCaja(turno);
    f.setTipoMovimientoCaja(tipo);
    f.setPago(p);
    f.setConcepto("Pago " + (reserva == null ? "venta V" + c.getVenta().getIdVenta() : "reserva R" + reserva)
        + " cuenta " + c.getIdComprobante());
    f.setMonto(p.getMonto());
    caja.saveAndFlush(f);
    return c;
  }

  @Override
  public Comprobante emitir(Comprobante cuenta, ComprobanteCuentaDTO request) {
    reglas.validar(request);
    reglas.usuario(request.idUsuario());
    var turnoEmision = gestion.turno(request.idTurno(), request.idUsuario());
    var c = bloquear(cuenta);
    if (c.getEstadoComprobante() == EstadoComprobante.EMITIDO) {
      exigir(c.getTipoComprobante().getCodigo().equals(request.tipo())
          && c.getCliente() != null && c.getCliente().getIdPerfil().equals(request.idClienteReceptor()),
          "El comprobante ya fue emitido con otro tipo o receptor");
      return c;
    }
    exigir(c.getEstadoComprobante() == EstadoComprobante.BORRADOR, "El documento no está en borrador");
    var receptor = clientes.findByIdPerfilAndEsCliente(request.idClienteReceptor(), 1)
        .orElseThrow(() -> new RecursoNoEncontradoException("El receptor no existe"));
    var perfil = receptor;
    if ("FACTURA".equals(request.tipo()))
      exigir("6".equals(perfil.getTipoDocumento().getCodigoSunat())
          && perfil.getNumeroDocumento().matches("[0-9]{11}"),
          "La factura interna requiere un receptor con RUC registrado");
    var tipo = tipos.findByCodigo(request.tipo())
        .orElseThrow(() -> new ReglaNegocioException("Falta configurar " + request.tipo()));
    String serie = "FACTURA".equals(request.tipo()) ? "FINT" : "BINT";
    var contador = series.bloquear(new SerieComprobanteId(tipo.getIdTipoComprobante(), serie))
        .orElseThrow(() -> new ReglaNegocioException("Falta configurar la serie " + serie));
    exigir(contador.getSiguienteNumero() < 9999999999L, "La serie agotó su numeración");
    c.setTipoComprobante(tipo);
    c.setSerie(serie);
    c.setNumeroComprobante(String.valueOf(contador.getSiguienteNumero()));
    contador.setSiguienteNumero(contador.getSiguienteNumero() + 1);
    c.setFechaEmision(ahora());
    c.setTurnoCaja(turnoEmision);
    c.setCliente(receptor);
    c.setReceptorTipoDoc(perfil.getTipoDocumento().getCodigoSunat() == null
        ? "OTRO" : perfil.getTipoDocumento().getCodigoSunat());
    c.setReceptorNumeroDoc(perfil.getNumeroDocumento());
    c.setReceptorNombre(personas.nombre(perfil));
    c.setEstadoComprobante(EstadoComprobante.EMITIDO);
    return cuentas.saveAndFlush(c);
  }

  private Comprobante bloquear(Comprobante cuenta) {
    var c = cuentas.bloquear(cuenta.getIdComprobante())
        .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta no existe"));
    em.refresh(c);
    return c;
  }

  private Long idReserva(Comprobante c) {
    if (c.getReserva() != null) return c.getReserva().getIdReserva();
    return c.getVenta() != null && c.getVenta().getDetalleReserva() != null
        ? c.getVenta().getDetalleReserva().getReserva().getIdReserva() : null;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
  public CuentaDocumentoDTO documento(Comprobante c) {
    var ps = pagos.findByComprobanteIdComprobante(c.getIdComprobante()).stream()
        .map(p -> new ReservaPagoRespuestaDTO(p.getIdPago(), p.getFormaPago().getIdFormaPago(),
            p.getTurnoCaja().getIdTurno(), p.getMonto(), p.getNumeroOperacion(), p.getFechaPago())).toList();
    var ls = lineas.findByComprobanteIdComprobanteOrderByNumeroLinea(c.getIdComprobante()).stream()
        .map(l -> new CuentaLineaDTO(l.getDetalleReserva() == null ? null : l.getDetalleReserva().getIdDetalleReserva(),
            l.getDetalleVenta() == null ? null : l.getDetalleVenta().getIdDetalleVenta(),
            l.getDescripcion(), l.getCantidad(), gestion.total(l))).toList();
    var pagado = gestion.pagado(c);
    Long reserva = idReserva(c);
    return new CuentaDocumentoDTO(c.getIdComprobante(), reserva, reserva == null ? null : "R" + reserva,
        c.getVenta() == null ? null : c.getVenta().getIdVenta(), c.getTipoComprobante().getCodigo(),
        c.getEstadoComprobante(), c.getSerie(), c.getNumeroComprobante(),
        c.getCliente() == null ? null : c.getCliente().getIdPerfil(), c.getReceptorNombre(),
        c.getReceptorNumeroDoc(), c.getFechaEmision(), c.getTotalPagar(), pagado,
        c.getTotalPagar().subtract(pagado), ls, ps);
  }
}
