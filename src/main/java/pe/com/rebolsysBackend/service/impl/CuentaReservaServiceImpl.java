package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.*;

import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.enums.*;
import pe.com.rebolsysBackend.exception.*;
import pe.com.rebolsysBackend.repository.*;
import pe.com.rebolsysBackend.service.service.*;

@Service
@RequiredArgsConstructor
public class CuentaReservaServiceImpl implements CuentaReservaService {
  private final CuentaReservaGestion gestion;
  private final CuentaCobroService cobros;
  private final ReservaRepository reservas;
  private final VentaRepository ventas;
  private final ComprobanteRepository cuentas;
  private final PagoRepository pagos;
  private final VentaService ventaService;
  private final ReglasPropuesta reglas;

  private Reserva buscar(Long id) {
    return reservas
        .findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("La reserva no existe"));
  }

  @Override
  @Transactional(readOnly = true)
  public CuentaReservaDTO cuenta(Long id) {
    var r = buscar(id);
    var docs = new ArrayList<CuentaDocumentoDTO>();
    var principal = gestion.principal(r);
    if (principal.isEmpty()) {
      var cargos =
          r.getDetalles().stream()
              .filter(d -> d.getEstadoDetalle() != EstadoReserva.CANCELADA)
              .map(
                  d ->
                      new CuentaLineaDTO(
                          d.getIdDetalleReserva(),
                          null,
                          "Alojamiento habitación " + d.getHabitacion().getNumeroCuarto(),
                          BigDecimal.ONE,
                          d.getCostoEstadia()))
              .toList();
      docs.add(
          new CuentaDocumentoDTO(
              null,
              id,
              "R" + id,
              null,
              "CUENTA",
              EstadoComprobante.BORRADOR,
              "CTA",
              null,
              r.getCliente().getIdPerfil(),
              null,
              null,
              null,
              gestion.alojamiento(r),
              BigDecimal.ZERO,
              gestion.alojamiento(r),
              cargos,
              List.of()));
    }
    cuentas.cuentasReserva(id).forEach(c -> docs.add(documento(c)));
    var total =
        docs.stream().map(CuentaDocumentoDTO::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    var pagado =
        docs.stream().map(CuentaDocumentoDTO::pagado).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new CuentaReservaDTO(
        id,
        "R" + id,
        gestion.alojamiento(r),
        total,
        pagado,
        total.subtract(pagado),
        gestion.admiteCargos(r),
        docs);
  }

  @Override
  @Transactional
  public CuentaReservaDTO pagar(Long id, ReservaPagoRegistroDTO request) {
    reglas.validar(request);
    var r = gestion.bloquear(id);
    reglas.usuario(request.idUsuario());
    var previo = pagos.findByClaveIdempotencia(request.claveOperacion());
    if (previo.isPresent()) {
      var p = previo.get();
      var c = p.getComprobante();
      boolean mismoAmbito =
          request.idVenta() == null
              ? c.getReserva() != null && c.getReserva().getIdReserva().equals(id)
              : c.getVenta() != null
                  && c.getVenta().getIdVenta().equals(request.idVenta())
                  && c.getVenta().getDetalleReserva() != null
                  && c.getVenta().getDetalleReserva().getReserva().getIdReserva().equals(id);
      exigir(
          mismoAmbito
              && p.getUsuario().getIdUsuario().equals(request.idUsuario())
              && p.getTurnoCaja().getIdTurno().equals(request.idTurno())
              && p.getFormaPago().getIdFormaPago().equals(request.idFormaPago())
              && p.getMonto().compareTo(request.monto()) == 0
              && Objects.equals(p.getNumeroOperacion(), limpiar(request.numeroOperacion())),
          "La clave de pago ya corresponde a otra solicitud");
      return cuenta(id);
    }
    var turno = gestion.turno(request.idTurno(), request.idUsuario());
    var c = cuentaObjetivo(r, request.idVenta(), turno);
    cobros.pagar(c, new PagoCuentaDTO(request.claveOperacion(), request.idUsuario(), request.idTurno(),
        request.idFormaPago(), request.monto(), request.numeroOperacion()));
    return cuenta(id);
  }

  private Comprobante cuentaObjetivo(Reserva r, Long idVenta, TurnoCaja turno) {
    if (idVenta == null) {
      exigir(
          r.getDetalles().stream().anyMatch(d -> d.getEstadoDetalle() != EstadoReserva.CANCELADA),
          "La reserva está anulada");
      return gestion.principal(r, turno);
    }
    var v = ventaDeReserva(r.getIdReserva(), idVenta);
    exigir(
        v.getModalidadCobro() == ModalidadCobro.SEPARADA,
        "Este consumo se paga en la cuenta principal");
    return gestion.cuentaVenta(v);
  }

  private Venta ventaDeReserva(Long idReserva, Long idVenta) {
    var v =
        ventas
            .findById(idVenta)
            .orElseThrow(() -> new RecursoNoEncontradoException("El consumo no existe"));
    exigir(
        v.getDetalleReserva() != null
            && v.getDetalleReserva().getReserva().getIdReserva().equals(idReserva),
        "El consumo no pertenece a esta reserva");
    return v;
  }

  @Override
  @Transactional
  public CuentaDocumentoDTO emitir(Long id, ReservaComprobanteRegistroDTO request) {
    reglas.validar(request);
    var r = gestion.bloquear(id);
    reglas.usuario(request.idUsuario());
    var turno = gestion.turno(request.idTurno(), request.idUsuario());
    var c = cuentaObjetivo(r, request.idVenta(), turno);
    return cobros.documento(cobros.emitir(c, new ComprobanteCuentaDTO(request.idUsuario(),
        request.idTurno(), request.tipo(), request.idClienteReceptor())));
  }

  @Override
  @Transactional
  public ConsumoReservaRespuestaDTO consumir(Long id, VentaRequestDTO request) {
    reglas.validar(request);
    gestion.bloquear(id);
    exigir(
        request.idDetalleReserva() != null, "Indica la habitación de la reserva para este consumo");
    // El servicio de ventas valida existencia, estado y stock; todo participa de esta transacción.
    var detalle =
        buscar(id).getDetalles().stream()
            .filter(d -> d.getIdDetalleReserva().equals(request.idDetalleReserva()))
            .findFirst();
    exigir(detalle.isPresent(), "La habitación del consumo no pertenece a esta reserva");
    var borrador = ventaService.registrar(request);
    var v =
        ventaService.confirmar(borrador.idVenta(), new OperacionUsuarioDTO(request.idUsuario()));
    return consumo(ventas.findById(v.idVenta()).orElseThrow());
  }

  @Override
  @Transactional(readOnly = true)
  public List<ConsumoReservaRespuestaDTO> consumos(Long id) {
    buscar(id);
    return ventas.findByDetalleReservaReservaIdReservaOrderByIdVenta(id).stream()
        .map(this::consumo)
        .toList();
  }

  private ConsumoReservaRespuestaDTO consumo(Venta v) {
    Long id = v.getDetalleReserva().getReserva().getIdReserva();
    Long idCuenta =
        v.getModalidadCobro() == ModalidadCobro.RESERVA
            ? gestion
                .principal(v.getDetalleReserva().getReserva())
                .map(Comprobante::getIdComprobante)
                .orElse(null)
            : cuentas
                .findByClaveIdempotencia("CUENTA-V-" + v.getIdVenta())
                .map(Comprobante::getIdComprobante)
                .orElse(null);
    return new ConsumoReservaRespuestaDTO(
        id, "R" + id, idCuenta, ventaService.obtener(v.getIdVenta()));
  }

  private CuentaDocumentoDTO documento(Comprobante c) {
    return cobros.documento(c);
  }
}
