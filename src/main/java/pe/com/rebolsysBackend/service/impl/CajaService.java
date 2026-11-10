package pe.com.rebolsysBackend.service.impl;

import static java.util.stream.Collectors.toMap;
import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.*;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.com.rebolsysBackend.dto.CajaDTO.*;
import pe.com.rebolsysBackend.dto.PaginaResponseDTO;
import pe.com.rebolsysBackend.dto.SesionActualDTO;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.exception.RecursoNoEncontradoException;
import pe.com.rebolsysBackend.repository.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CajaService {
  private static final String INGRESO_MANUAL = "INGRESO_MANUAL";
  private static final BigDecimal MAX_ARQUEO = new BigDecimal("99999999.99");
  private final EntityManager em;
  private final SesionOperacionService sesion;
  private final TurnoCajaRepository turnos;
  private final ArqueoCajaRepository arqueos;
  private final FormaPagoRepository formas;
  private final PagoRepository pagos;
  private final FlujoCajaRepository flujos;
  private final TipoMovimientoCajaRepository tipos;
  private final EvidenciaCajaService evidencias;
  private final AuditoriaOperacionService auditoria;

  public record Descarga(Resource archivo, String nombre) {}

  public Contexto actual() {
    var actor = sesion.actual();
    if (actor.idTurno() != null) return contexto(turnoPropio(actor.idTurno(), actor));
    exigir(turnos.findByUsuarioIdUsuarioAndEstadoSesionAndFechaCierreIsNullOrderByIdTurno(actor.idUsuario(), 1).size() <= 1,
        "Tu usuario tiene más de un turno abierto. Revisa los turnos antes de operar la caja");
    return turnos.findFirstByUsuarioIdUsuarioAndEstadoSesionAndFechaCierreIsNotNullOrderByIdTurnoDesc(
        actor.idUsuario(), 0).map(this::contexto).orElse(null);
  }

  public Contexto consultar(Long id) {
    return contexto(turnoPropio(id, sesion.actual()));
  }

  public PaginaResponseDTO<Movimiento> movimientos(Long id, int pagina, int tamanio) {
    turnoPropio(id, sesion.actual());
    var resultado = flujos.findByTurnoCajaIdTurno(id,
        PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "fechaHora", "idFlujo")));
    return new PaginaResponseDTO<>(resultado.getContent().stream().map(this::movimiento).toList(),
        resultado.getNumber(), resultado.getSize(), resultado.getTotalElements(), resultado.getTotalPages(), resultado.isLast());
  }

  @Transactional
  public Movimiento registrar(Long id, RegistrarMovimiento datos, MultipartFile adjunto, String ip) {
    var actor = sesion.actual();
    var turno = bloquearTurnoPropio(id, actor);
    var tipo = tipos.findById(datos.idTipoMovimiento()).filter(this::tipoManual)
        .orElseThrow(() -> new RecursoNoEncontradoException("La categoría de caja no está disponible"));
    if (!actor.tiene(tipo.getSigno() == -1 ? "GASTOS:CREAR" : "CAJA:REGISTRAR_INGRESO"))
      throw new AccessDeniedException("Tu usuario no tiene permiso para registrar este movimiento");

    var archivo = evidencias.preparar(adjunto);
    var previo = flujos.findByClaveRegistro(datos.claveRegistro());
    if (previo.isPresent()) {
      var anterior = previo.get();
      exigir(anterior.getPago() == null && anterior.getTurnoCaja().getIdTurno().equals(id)
          && anterior.getTipoMovimientoCaja().getIdTipoMovimiento().equals(datos.idTipoMovimiento())
          && anterior.getConcepto().equals(datos.concepto().strip())
          && anterior.getMonto().compareTo(datos.monto()) == 0
          && Objects.equals(anterior.getDocumentoRespaldo(), archivo == null ? null : archivo.nombre())
          && Objects.equals(anterior.getEvidenciaSha256(), archivo == null ? null : archivo.sha()),
          "La clave de registro ya corresponde a otro movimiento");
      return movimiento(anterior);
    }
    exigirTurnoVigente(turno, actor);

    var flujo = new FlujoCaja();
    flujo.setTurnoCaja(turno);
    flujo.setTipoMovimientoCaja(tipo);
    flujo.setConcepto(datos.concepto().strip());
    flujo.setMonto(datos.monto());
    flujo.setClaveRegistro(datos.claveRegistro());
    if (archivo != null) {
      flujo.setEvidenciaRuta(evidencias.guardar(archivo));
      flujo.setDocumentoRespaldo(archivo.nombre());
      flujo.setEvidenciaSha256(archivo.sha());
    }
    flujos.saveAndFlush(flujo);
    auditoria.registrar(actor, "FLUJO_CAJA", flujo.getIdFlujo(), "POST", ruta(id) + "/movimientos", ip);
    return movimiento(flujo);
  }

  @Transactional
  public Contexto cerrar(Long id, Cerrar datos, String ip) {
    var actor = sesion.actual();
    var turno = bloquearTurnoPropio(id, actor);
    exigirTurnoVigente(turno, actor);
    var actual = contexto(turno);
    exigir(actual.bloqueoCierre() == null, actual.bloqueoCierre());
    exigir(actual.revision().equals(datos.revision()), "Los importes cambiaron durante el conteo. Actualiza el arqueo y vuelve a revisarlo");
    Map<Long, BigDecimal> declarados = new HashMap<>();
    for (var declarado : datos.arqueos()) {
      exigir(declarados.putIfAbsent(declarado.idFormaPago(), declarado.montoDeclarado()) == null,
          "Declara cada forma de pago una sola vez");
    }
    exigir(declarados.keySet().equals(new HashSet<>(actual.arqueos().stream().map(Arqueo::idFormaPago).toList())),
        "Completa el arqueo de todas las formas de pago mostradas");
    var cierre = ahora();
    exigir(!cierre.isBefore(turno.getFechaApertura()), "La fecha de cierre es anterior a la apertura del turno");

    var filas = actual.arqueos().stream().map(a -> {
      var fila = new ArqueoCaja();
      fila.setTurnoCaja(turno);
      fila.setFormaPago(formas.getReferenceById(a.idFormaPago()));
      fila.setMontoCalculado(a.montoCalculado());
      fila.setMontoDeclarado(declarados.get(a.idFormaPago()));
      return fila;
    }).toList();
    turno.setFechaCierre(cierre);
    turno.setEstadoSesion(0);
    arqueos.saveAllAndFlush(filas);
    auditoria.registrar(actor, "TURNO_CAJA", id, "POST", ruta(id) + "/cierre", ip);
    return contexto(turno);
  }

  public Descarga evidencia(Long idFlujo) {
    var flujo = flujos.findById(idFlujo).orElseThrow(() -> new RecursoNoEncontradoException("La evidencia no existe"));
    turnoPropio(flujo.getTurnoCaja().getIdTurno(), sesion.actual());
    return new Descarga(evidencias.obtener(flujo.getEvidenciaRuta()), flujo.getDocumentoRespaldo());
  }

  private Contexto contexto(TurnoCaja turno) {
    Long id = turno.getIdTurno();
    var manuales = flujos.sumarManuales(id).stream().collect(toMap(FlujoCajaRepository.ImporteManual::getSigno,
        FlujoCajaRepository.ImporteManual::getMonto));
    var ingresos = manuales.getOrDefault(1, BigDecimal.ZERO);
    var gastos = manuales.getOrDefault(-1, BigDecimal.ZERO);
    var guardados = arqueos.findByTurnoCajaIdTurnoOrderByFormaPagoIdFormaPago(id);
    boolean abierto = abierto(turno);
    List<Tipo> disponibles = abierto ? tipos.findAll(Sort.by("nombreMovimiento")).stream().filter(this::tipoManual)
        .map(t -> new Tipo(t.getIdTipoMovimiento(), t.getSigno() == 1 ? "Ingreso manual" : t.getNombreMovimiento(),
            t.getSigno())).toList() : List.of();
    List<Arqueo> filas;
    String bloqueo = null;
    String revision = null;
    if (!abierto || !guardados.isEmpty()) {
      filas = guardados.stream().map(a -> new Arqueo(a.getFormaPago().getIdFormaPago(), a.getFormaPago().getNombrePago(),
          a.getMontoCalculado(), a.getMontoDeclarado(), a.getDiferencia())).toList();
      if (abierto) bloqueo = "El turno ya tiene arqueos guardados y requiere revisión antes del cierre";
    } else {
      var medios = formas.findAll(Sort.by("idFormaPago"));
      var cobros = pagos.sumarVerificados(id).stream().collect(toMap(PagoRepository.ImporteForma::getIdFormaPago,
          PagoRepository.ImporteForma::getMonto));
      var ajusteEfectivo = turno.getMontoBase().add(ingresos).subtract(gastos);
      filas = medios.stream().map(forma -> {
        var importe = cobros.getOrDefault(forma.getIdFormaPago(), BigDecimal.ZERO);
        if (efectivo(forma)) importe = importe.add(ajusteEfectivo);
        return new Arqueo(forma.getIdFormaPago(), forma.getNombrePago(), importe, null, null);
      }).toList();
      if (medios.stream().filter(this::efectivo).count() != 1) bloqueo = "Configura una única forma de pago Efectivo para cerrar el turno";
      else if (filas.stream().anyMatch(a -> a.montoCalculado().signum() < 0 || a.montoCalculado().compareTo(MAX_ARQUEO) > 0))
        bloqueo = "Hay un importe esperado negativo o fuera del rango de arqueo. Revisa los movimientos antes de cerrar";
      if (bloqueo == null) revision = hash(id + ":" + filas.stream()
          .map(a -> a.idFormaPago() + "=" + a.montoCalculado().stripTrailingZeros().toPlainString()).toList());
    }
    return new Contexto(id, turno.getFechaApertura(), turno.getFechaCierre(), turno.getMontoBase(), abierto,
        disponibles, ingresos, gastos, filas, revision, bloqueo);
  }

  private TurnoCaja turnoPropio(Long id, SesionActualDTO actor) {
    return turnos.findById(id).filter(t -> t.getUsuario().getIdUsuario().equals(actor.idUsuario()))
        .orElseThrow(() -> new RecursoNoEncontradoException("El turno no está disponible"));
  }

  private TurnoCaja bloquearTurnoPropio(Long id, SesionActualDTO actor) {
    turnoPropio(id, actor);
    var turno = turnos.bloquear(id).orElseThrow(() -> new RecursoNoEncontradoException("El turno no está disponible"));
    em.refresh(turno);
    return turno;
  }

  private void exigirTurnoVigente(TurnoCaja turno, SesionActualDTO actor) {
    exigir(abierto(turno), "El turno ya está cerrado");
    exigir(turno.getIdTurno().equals(actor.idTurno()), "Se necesita un único turno abierto para operar la caja");
  }

  private boolean abierto(TurnoCaja turno) {
    return turno.getEstadoSesion() == 1 && turno.getFechaCierre() == null;
  }

  private boolean efectivo(FormaPago forma) {
    return "EFECTIVO".equalsIgnoreCase(forma.getNombrePago().strip());
  }

  private boolean tipoManual(TipoMovimientoCaja tipo) {
    return tipo.getSigno() == -1 || (tipo.getSigno() == 1 && INGRESO_MANUAL.equals(tipo.getNombreMovimiento()));
  }

  private Movimiento movimiento(FlujoCaja flujo) {
    var tipo = flujo.getTipoMovimientoCaja();
    var nombre = INGRESO_MANUAL.equals(tipo.getNombreMovimiento()) ? "Ingreso manual" : tipo.getNombreMovimiento();
    String evidencia = EvidenciaCajaService.esRutaCaja(flujo.getEvidenciaRuta()) ? "/api/v1/caja/movimientos/" + flujo.getIdFlujo() + "/evidencia" : null;
    return new Movimiento(flujo.getIdFlujo(), nombre, tipo.getSigno(), flujo.getConcepto(), flujo.getMonto(),
        flujo.getFechaHora(), flujo.getDocumentoRespaldo(), evidencia);
  }

  private String ruta(Long id) { return "/api/v1/caja/turnos/" + id; }
}
