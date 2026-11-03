package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.*;

import jakarta.persistence.EntityManager;
import java.math.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.enums.*;
import pe.com.rebolsysBackend.exception.*;
import pe.com.rebolsysBackend.repository.*;

/** Operaciones de cuenta bajo el bloqueo de la reserva y una transacción del servicio. */
@Component
@RequiredArgsConstructor
public class CuentaReservaGestion {
  private final EntityManager em;
  private final ReservaRepository reservas;
  private final DetalleReservaRepository detallesReserva;
  private final ComprobanteRepository cuentas;
  private final ComprobanteDetalleRepository lineas;
  private final PagoRepository pagos;
  private final TipoComprobanteRepository tipos;
  private final SerieComprobanteRepository series;
  private final TurnoCajaRepository turnos;

  public Reserva bloquear(Long id) {
    var r = reservas.bloquear(id).orElseThrow(() -> new RecursoNoEncontradoException("La reserva no existe"));
    return r;
  }

  public BigDecimal alojamiento(Reserva r) {
    return detallesReserva.findByReservaIdReservaOrderByIdDetalleReserva(r.getIdReserva()).stream()
        .filter(d -> d.getEstadoDetalle() != EstadoReserva.CANCELADA)
        .map(DetalleReserva::getCostoEstadia)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public Optional<Comprobante> principal(Reserva r) {
    return cuentas.findByClaveIdempotencia(clavePrincipal(r));
  }

  public String clavePrincipal(Reserva r) {
    return "CUENTA-R-" + r.getIdReserva();
  }

  public BigDecimal pagado(Comprobante c) {
    return pagos.findByComprobanteIdComprobante(c.getIdComprobante()).stream()
        .filter(p -> p.getVerificado() == 1)
        .map(Pago::getMonto)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public BigDecimal saldo(Comprobante c) {
    return c.getTotalPagar().subtract(pagado(c));
  }

  public boolean admiteCargos(Reserva r) {
    return principal(r)
        .map(c -> c.getEstadoComprobante() == EstadoComprobante.BORRADOR && saldo(c).signum() > 0)
        .orElseGet(() -> alojamiento(r).signum() > 0);
  }

  public TurnoCaja turno(Long id, Long usuario) {
    var t =
        turnos
            .bloquear(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("El turno no existe"));
    em.refresh(t);
    exigir(
        t.getEstadoSesion() == 1 && t.getFechaCierre() == null,
        "El turno de caja debe estar abierto");
    exigir(t.getUsuario().getIdUsuario().equals(usuario), "El turno pertenece a otro usuario");
    return t;
  }

  public TurnoCaja turnoOperador(Long usuario) {
    var abiertos =
        turnos.findByUsuarioIdUsuarioAndEstadoSesionAndFechaCierreIsNullOrderByIdTurno(usuario, 1);
    exigir(
        abiertos.size() == 1,
        "El operador debe tener exactamente un turno abierto para registrar consumos");
    return turno(abiertos.getFirst().getIdTurno(), usuario);
  }

  public Comprobante principal(Reserva r, TurnoCaja turno) {
    return principal(r)
        .orElseGet(
            () -> {
              var c = nueva(turno, clavePrincipal(r));
              c.setReserva(r);
              c.setCliente(r.getCliente());
              cuentas.saveAndFlush(c);
              int n = 0;
              for (var d :
                  detallesReserva.findByReservaIdReservaOrderByIdDetalleReserva(r.getIdReserva())) {
                if (d.getEstadoDetalle() == EstadoReserva.CANCELADA) continue;
                var l =
                    base(
                        c,
                        ++n,
                        "Alojamiento habitación " + d.getHabitacion().getNumeroCuarto(),
                        BigDecimal.ONE,
                        d.getCostoEstadia(),
                        BigDecimal.ZERO);
                l.setDetalleReserva(d);
                lineas.save(l);
              }
              recalcular(c);
              return c;
            });
  }

  private Comprobante nueva(TurnoCaja turno, String clave) {
    var tipo =
        tipos
            .findByCodigo("CUENTA")
            .orElseThrow(
                () -> new ReglaNegocioException("Falta configurar el tipo CUENTA de desarrollo"));
    var serie =
        series.findById(
            new pe.com.rebolsysBackend.entity.id.SerieComprobanteId(
                tipo.getIdTipoComprobante(), "CTA"));
    exigir(serie.isPresent(), "Falta configurar la serie CTA de desarrollo");
    var c = new Comprobante();
    c.setTipoComprobante(tipo);
    c.setSerie("CTA");
    c.setTurnoCaja(turno);
    c.setClaveIdempotencia(clave);
    c.setEstadoComprobante(EstadoComprobante.BORRADOR);
    c.setSubtotal(BigDecimal.ZERO);
    c.setIgv(BigDecimal.ZERO);
    c.setTotalPagar(BigDecimal.ZERO);
    return c;
  }

  public Comprobante cuentaVenta(Venta v) {
    return cuentas
        .findByClaveIdempotencia("CUENTA-V-" + v.getIdVenta())
        .orElseThrow(() -> new ReglaNegocioException("El consumo no tiene cuenta separada"));
  }

  public Comprobante cuentaVenta(Venta v, TurnoCaja turno) {
    exigir(v.getModalidadCobro() == ModalidadCobro.SEPARADA,
        "Este consumo se paga en la cuenta principal de la reserva");
    exigir(v.getEstadoVenta() == EstadoVenta.CONFIRMADA, "Confirma la venta antes de cobrarla");
    var existente = cuentas.findByClaveIdempotencia("CUENTA-V-" + v.getIdVenta());
    if (existente.isPresent()) return existente.get();
    var c = nueva(turno, "CUENTA-V-" + v.getIdVenta());
    c.setVenta(v);
    c.setCliente(v.getClienteComprador());
    cuentas.saveAndFlush(c);
    int n = 0;
    for (var d : v.getDetalles()) {
      var l = base(c, ++n, d.getDescripcion(), BigDecimal.valueOf(d.getCantidad()), d.getValorUnitario(), d.getIgv());
      l.setDetalleVenta(d);
      lineas.save(l);
    }
    recalcular(c);
    return c;
  }

  public void validarVenta(Venta v) {
    if (v.getDetalleReserva() == null) return;
    var r = bloquear(v.getDetalleReserva().getReserva().getIdReserva());
    if (v.getModalidadCobro() == ModalidadCobro.RESERVA)
      exigir(
          admiteCargos(r),
          "La cuenta de reserva está pagada o emitida; registra el consumo con modalidad SEPARADA");
  }

  public Comprobante registrarVenta(Venta v, Long operador) {
    if (v.getDetalleReserva() == null) return null;
    var r = bloquear(v.getDetalleReserva().getReserva().getIdReserva());
    var existentes = lineas.existsByDetalleVentaVentaIdVenta(v.getIdVenta());
    if (existentes)
      return v.getModalidadCobro() == ModalidadCobro.RESERVA
          ? principal(r).orElseThrow()
          : cuentaVenta(v);
    validarVenta(v);
    var turno = turnoOperador(operador);
    if (v.getModalidadCobro() == ModalidadCobro.SEPARADA) return cuentaVenta(v, turno);
    var c = principal(r, turno);
    int n = lineas.findByComprobanteIdComprobanteOrderByNumeroLinea(c.getIdComprobante()).size();
    for (var d : v.getDetalles()) {
      var l =
          base(
              c,
              ++n,
              d.getDescripcion(),
              BigDecimal.valueOf(d.getCantidad()),
              d.getValorUnitario(),
              d.getIgv());
      l.setDetalleVenta(d);
      lineas.save(l);
    }
    recalcular(c);
    return c;
  }

  private ComprobanteDetalle base(
      Comprobante c,
      int n,
      String descripcion,
      BigDecimal cantidad,
      BigDecimal precio,
      BigDecimal igv) {
    exigir(n <= 9999, "La cuenta alcanzó el máximo de líneas");
    var l = new ComprobanteDetalle();
    l.setComprobante(c);
    l.setNumeroLinea(n);
    l.setDescripcion(descripcion);
    l.setCantidad(cantidad);
    l.setValorUnitario(precio);
    l.setIgv(igv);
    return l;
  }

  public BigDecimal total(ComprobanteDetalle l) {
    return l.getCantidad()
        .multiply(l.getValorUnitario())
        .setScale(2, RoundingMode.HALF_UP)
        .add(l.getIgv());
  }

  public void recalcular(Comprobante c) {
    var ls = lineas.findByComprobanteIdComprobanteOrderByNumeroLinea(c.getIdComprobante());
    var total = ls.stream().map(this::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    var igv = ls.stream().map(ComprobanteDetalle::getIgv).reduce(BigDecimal.ZERO, BigDecimal::add);
    exigir(
        total.compareTo(new BigDecimal("9999999999.99")) <= 0,
        "El total de la cuenta supera el máximo");
    c.setSubtotal(total.subtract(igv));
    c.setIgv(igv);
    c.setTotalPagar(total);
    cuentas.saveAndFlush(c);
  }

  public void validarSalida(Reserva r) {
    var principal = principal(r);
    BigDecimal pendiente = principal.map(this::saldo).orElseGet(() -> alojamiento(r));
    for (var c : cuentas.cuentasReserva(r.getIdReserva()))
      if (c.getVenta() != null) pendiente = pendiente.add(saldo(c));
    exigir(
        pendiente.signum() == 0, "Liquida el saldo de alojamiento y consumos antes del check-out");
  }

  public void cancelarDetalle(DetalleReserva d) {
    var c = principal(d.getReserva());
    if (c.isEmpty()) return;
    exigir(
        c.get().getEstadoComprobante() == EstadoComprobante.BORRADOR
            && pagado(c.get()).signum() == 0,
        "La reserva tiene pagos o comprobante emitido; requiere gestionar devolución o"
            + " rectificación");
    for (var l : lineas.findByDetalleReservaIdDetalleReserva(d.getIdDetalleReserva()))
      lineas.delete(l);
    lineas.flush();
    recalcular(c.get());
  }
}
