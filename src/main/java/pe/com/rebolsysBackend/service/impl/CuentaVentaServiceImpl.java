package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.exigir;

import jakarta.persistence.EntityManager;
import java.math.*;
import java.util.List;
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
public class CuentaVentaServiceImpl implements CuentaVentaService {
  private final EntityManager em;
  private final VentaRepository ventas;
  private final ComprobanteRepository cuentas;
  private final CuentaReservaGestion gestion;
  private final CuentaCobroService cobros;
  private final ReglasPropuesta reglas;

  private Venta buscar(Long id) {
    return ventas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("La venta no existe"));
  }

  private void validar(Venta v) {
    exigir(v.getEstadoVenta() == EstadoVenta.CONFIRMADA, "Confirma la venta antes de cobrarla");
    exigir(v.getModalidadCobro() == ModalidadCobro.SEPARADA,
        "Este consumo se paga en la cuenta principal de la reserva");
  }

  private Venta bloquear(Long id) {
    var previo = buscar(id);
    if (previo.getDetalleReserva() != null)
      gestion.bloquear(previo.getDetalleReserva().getReserva().getIdReserva());
    var v = ventas.bloquear(id).orElseThrow();
    em.refresh(v);
    validar(v);
    return v;
  }

  @Override
  @Transactional(readOnly = true)
  public CuentaDocumentoDTO cuenta(Long id) {
    var v = buscar(id);
    validar(v);
    var existente = cuentas.findByClaveIdempotencia("CUENTA-V-" + id);
    if (existente.isPresent()) return cobros.documento(existente.get());
    Long reserva = v.getDetalleReserva() == null ? null : v.getDetalleReserva().getReserva().getIdReserva();
    var lineas = v.getDetalles().stream().map(d -> new CuentaLineaDTO(null, d.getIdDetalleVenta(),
            d.getDescripcion(), BigDecimal.valueOf(d.getCantidad()), d.getValorUnitario()
            .multiply(BigDecimal.valueOf(d.getCantidad())).setScale(2, RoundingMode.HALF_UP).add(d.getIgv()))).toList();
    var total = lineas.stream().map(CuentaLineaDTO::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new CuentaDocumentoDTO(null, reserva, reserva == null ? null : "R" + reserva, id, "CUENTA",
            EstadoComprobante.BORRADOR, "CTA", null, v.getClienteComprador() == null ? null : v.getClienteComprador().getIdCliente(),
            null, null, null, total, BigDecimal.ZERO, total, lineas, List.of());
  }

  @Override
  @Transactional
  public CuentaDocumentoDTO pagar(Long id, PagoCuentaDTO request) {
    reglas.validar(request);
    var v = bloquear(id);
    var c = cuentas.findByClaveIdempotencia("CUENTA-V-" + id)
            .orElseGet(() -> gestion.cuentaVenta(v, gestion.turno(request.idTurno(), request.idUsuario())));
    return cobros.documento(cobros.pagar(c, request));
  }

  @Override
  @Transactional
  public CuentaDocumentoDTO emitir(Long id, ComprobanteCuentaDTO request) {
    reglas.validar(request);
    var v = bloquear(id);
    var c = cuentas.findByClaveIdempotencia("CUENTA-V-" + id)
            .orElseGet(() -> gestion.cuentaVenta(v, gestion.turno(request.idTurno(), request.idUsuario())));
    return cobros.documento(cobros.emitir(c, request));
  }
}
