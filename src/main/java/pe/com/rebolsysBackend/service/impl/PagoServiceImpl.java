package pe.com.rebolsysBackend.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.entity.Cliente;
import pe.com.rebolsysBackend.entity.Comprobante;
import pe.com.rebolsysBackend.entity.ComprobanteDetalle;
import pe.com.rebolsysBackend.entity.Consumo;
import pe.com.rebolsysBackend.entity.DetalleReserva;
import pe.com.rebolsysBackend.entity.FlujoCaja;
import pe.com.rebolsysBackend.entity.Pago;
import pe.com.rebolsysBackend.entity.Perfil;
import pe.com.rebolsysBackend.entity.TipoComprobante;
import pe.com.rebolsysBackend.entity.TurnoCaja;
import pe.com.rebolsysBackend.entity.Usuario;
import pe.com.rebolsysBackend.entity.id.SerieComprobanteId;
import pe.com.rebolsysBackend.enums.EstadoCargo;
import pe.com.rebolsysBackend.enums.EstadoCobro;
import pe.com.rebolsysBackend.enums.EstadoComprobante;
import pe.com.rebolsysBackend.enums.TipoConsumo;
import pe.com.rebolsysBackend.exception.ReglaNegocioException;
import pe.com.rebolsysBackend.repository.ComprobanteDetalleRepository;
import pe.com.rebolsysBackend.repository.ComprobanteRepository;
import pe.com.rebolsysBackend.repository.ConsumoRepository;
import pe.com.rebolsysBackend.repository.FlujoCajaRepository;
import pe.com.rebolsysBackend.repository.FormaPagoRepository;
import pe.com.rebolsysBackend.repository.PagoRepository;
import pe.com.rebolsysBackend.repository.SerieComprobanteRepository;
import pe.com.rebolsysBackend.repository.TipoComprobanteRepository;
import pe.com.rebolsysBackend.repository.TipoMovimientoCajaRepository;
import pe.com.rebolsysBackend.repository.TurnoCajaRepository;
import pe.com.rebolsysBackend.service.service.ClienteService;
import pe.com.rebolsysBackend.service.service.ComprobanteService;
import pe.com.rebolsysBackend.service.service.PagoService;

@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final FlujoCajaRepository flujos;
    private final ComprobanteDetalleRepository comprobanteDetalleRepository;
    private final ComprobanteRepository comprobanteRepository;
    private final TipoMovimientoCajaRepository tiposMovimiento;
    private final FormaPagoRepository formasPago;
    private final TurnoCajaRepository turnos;
    private final TipoComprobanteRepository tiposComprobante;
    private final EntityManager em;
    private final PagoRepository pagos;
    private final SerieComprobanteRepository series;
    private final ConsumoRepository consumos;
    private final ClienteService clientes;
    private final ComprobanteService comprobantes;

    @Override
    @Transactional
    public BigDecimal cobrar(List<DetalleReserva> rows, String key, BigDecimal recibido, BigDecimal montoPago) {
        var recibos = new java.util.ArrayList<>(comprobantes.recibos(rows));
        var claves = new java.util.ArrayList<String>();
        claves.add(hash(key));
        recibos.forEach(c -> claves.add(hash(key + ":comprobante:" + c.getIdComprobante())));
        var anteriores = pagos.findByClaveIdempotenciaIn(claves);
        if (!anteriores.isEmpty()) {
            if (
                anteriores
                    .stream()
                    .anyMatch(p ->
                        recibos
                            .stream()
                            .noneMatch(c -> c.getIdComprobante().equals(p.getComprobante().getIdComprobante()))
                    )
            ) throw new ReglaNegocioException("La clave de pago ya fue utilizada con otra reserva o importe");
            BigDecimal aplicado = anteriores.stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (montoPago != null && montoPago.compareTo(aplicado) != 0) throw new ReglaNegocioException(
                "La clave de pago ya fue utilizada con otra reserva o importe"
            );
            return BigDecimal.ZERO;
        }
        BigDecimal saldo = total(rows).subtract(comprobantes.pagado(rows));
        if (saldo.signum() == 0 && montoPago == null) return BigDecimal.ZERO;
        BigDecimal monto = montoPago == null ? saldo : montoPago;
        if (saldo.signum() <= 0) throw new ReglaNegocioException("La reserva ya está pagada; no tiene saldo pendiente");
        validarEfectivo(monto, recibido);
        if (monto.compareTo(saldo) > 0) throw new ReglaNegocioException(
            "El importe del pago no puede superar el saldo pendiente"
        );
        Usuario actor = rows.getFirst().getReserva().getUsuario();
        TurnoCaja turno = turnoAbierto(actor);
        var pendientes = rows
            .stream()
            .flatMap(r -> consumos.findByDetalleReservaIdDetalleReserva(r.getIdDetalleReserva()).stream())
            .filter(c -> c.getEstadoCargo() == EstadoCargo.PENDIENTE)
            .toList();
        if (!pendientes.isEmpty()) recibos.add(
            emitir(
                pendientes,
                rows.getFirst().getReserva().getCliente(),
                turno,
                hash(key + ":emision:" + pendientes.stream().map(Consumo::getIdConsumo).toList()),
                null
            )
        );
        recibos.sort(java.util.Comparator.comparing(Comprobante::getIdComprobante));
        BigDecimal restante = monto;
        boolean primero = true;
        for (Comprobante recibo : recibos) {
            if (recibo.getEstadoComprobante() != EstadoComprobante.EMITIDO) continue;
            BigDecimal deuda = recibo.getTotalPagar().subtract(pagado(recibo));
            if (deuda.signum() <= 0 || restante.signum() <= 0) continue;
            BigDecimal aplicar = deuda.min(restante);
            registrarPago(
                recibo,
                actor,
                turno,
                aplicar,
                primero ? hash(key) : hash(key + ":comprobante:" + recibo.getIdComprobante()),
                "INGRESO RESERVA",
                "Abono de reserva " + referencia(rows.getFirst())
            );
            primero = false;
            restante = restante.subtract(aplicar);
            actualizarEstadoCobro(recibo);
        }
        if (restante.signum() != 0) throw new ReglaNegocioException(
            "Los comprobantes no coinciden con la deuda de la reserva; revisa los consumos"
        );
        return recibido.subtract(monto);
    }

    @Override
    @Transactional
    public Comprobante cobrarVenta(
        List<Consumo> cargos,
        Cliente cliente,
        Usuario actor,
        String compradorNombre,
        String key,
        BigDecimal recibido
    ) {
        BigDecimal total = cargos.stream().map(Consumo::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() != 0) validarEfectivo(total, recibido);
        var turno = turnoAbierto(actor);
        var recibo = emitir(cargos, cliente, turno, hash("venta:" + key + ":comprobante"), compradorNombre);
        if (total.signum() > 0) registrarPago(
            recibo,
            actor,
            turno,
            total,
            hash("venta:" + key),
            "INGRESO VENTA",
            "Cobro de venta de productos"
        );
        actualizarEstadoCobro(recibo);
        return recibo;
    }

    private void validarEfectivo(BigDecimal monto, BigDecimal recibido) {
        if (
            monto.signum() <= 0 || monto.compareTo(new BigDecimal("99999999.99")) > 0 || monto.scale() > 2
        ) throw new ReglaNegocioException("El importe del pago debe ser positivo y admitir como máximo dos decimales");
        if (recibido == null || recibido.compareTo(monto) < 0) throw new ReglaNegocioException(
            "El efectivo recibido es menor al importe del pago"
        );
    }

    private BigDecimal pagado(Comprobante recibo) {
        return pagos
            .findByComprobanteIdComprobante(recibo.getIdComprobante())
            .stream()
            .map(Pago::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void actualizarEstadoCobro(Comprobante recibo) {
        var estado = pagado(recibo).compareTo(recibo.getTotalPagar()) >= 0 ? EstadoCobro.PAGADO : EstadoCobro.PENDIENTE;
        comprobanteDetalleRepository
            .findByComprobanteOrderByNumeroLinea(recibo)
            .forEach(d -> d.getConsumo().setEstadoCobro(estado));
    }

    private void registrarPago(
        Comprobante recibo,
        Usuario actor,
        TurnoCaja turno,
        BigDecimal monto,
        String key,
        String nombreMovimiento,
        String concepto
    ) {
        var efectivo = formasPago
            .findByNombrePagoIgnoreCase("EFECTIVO")
            .stream()
            .findFirst()
            .orElseThrow(() -> new ReglaNegocioException("No existe la forma de pago EFECTIVO"));
        var ingreso = tiposMovimiento
            .findByNombreMovimiento(nombreMovimiento)
            .orElseThrow(() -> new ReglaNegocioException("No existe el tipo de movimiento " + nombreMovimiento));
        if (ingreso.getSigno() != 1) throw new ReglaNegocioException("El movimiento de cobro debe ser un ingreso");
        var pago = new Pago();
        pago.setComprobante(recibo);
        pago.setFormaPago(efectivo);
        pago.setUsuario(actor);
        pago.setTurnoCaja(turno);
        pago.setMonto(monto);
        pago.setVerificado(1);
        pago.setClaveIdempotencia(key);
        pagos.save(pago);
        var flujo = new FlujoCaja();
        flujo.setTurnoCaja(turno);
        flujo.setPago(pago);
        flujo.setMonto(monto);
        flujo.setTipoMovimientoCaja(ingreso);
        flujo.setConcepto(concepto);
        flujos.save(flujo);
    }

    @Override
    @Transactional
    public void devolver(List<DetalleReserva> rows, String motivo) {
        var recibos = comprobantes.recibos(rows);
        var egreso = tiposMovimiento
            .findByNombreMovimiento("DEVOLUCION RESERVA")
            .orElseThrow(() -> new ReglaNegocioException("Falta registrar DEVOLUCION RESERVA en el catálogo de caja"));
        if (egreso.getSigno() != -1) throw new ReglaNegocioException(
            "La devolución debe registrarse como egreso de caja"
        );
        TurnoCaja turno = turnoAbierto(rows.getFirst().getReserva().getUsuario());
        for (Comprobante recibo : recibos) {
            for (Pago pago : pagos.findByComprobanteIdComprobante(recibo.getIdComprobante())) {
                if (!"EFECTIVO".equalsIgnoreCase(pago.getFormaPago().getNombrePago())) throw new ReglaNegocioException(
                    "Esta operación solo devuelve pagos en efectivo"
                );
                BigDecimal devuelto = flujos
                    .findByPagoIdPagoAndTipoMovimientoCajaSigno(pago.getIdPago(), -1)
                    .stream()
                    .map(FlujoCaja::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal pendiente = pago.getMonto().subtract(devuelto);
                if (pendiente.signum() < 0) throw new ReglaNegocioException(
                    "La devolución registrada excede el pago original; revisa caja"
                );
                if (pendiente.signum() == 0) continue;
                var flujo = new FlujoCaja();
                flujo.setTurnoCaja(turno);
                flujo.setPago(pago);
                flujo.setTipoMovimientoCaja(egreso);
                flujo.setMonto(pendiente);
                flujo.setConcepto("Devolución de reserva " + referencia(rows.getFirst()) + ": " + motivo.trim());
                flujo.setDocumentoRespaldo(recibo.getSerie() + "-" + recibo.getNumeroComprobante());
                flujos.save(flujo);
            }
            recibo.setEstadoComprobante(EstadoComprobante.ANULADO);
        }
    }

    private TurnoCaja turnoAbierto(Usuario actor) {
        em.lock(actor, LockModeType.PESSIMISTIC_WRITE);
        var abiertos = turnos.findByUsuarioAndFechaCierreIsNullOrderByIdTurno(actor);
        if (abiertos.isEmpty()) throw new ReglaNegocioException(
            "No existe un turno de caja abierto para el usuario de la reserva"
        );
        TurnoCaja turno = abiertos.getFirst();
        em.lock(turno, LockModeType.PESSIMISTIC_WRITE);
        if (turno.getFechaCierre() != null) throw new ReglaNegocioException("El turno de caja ya está cerrado");
        return turno;
    }

    private Comprobante emitir(
        List<Consumo> cargos,
        Cliente cliente,
        TurnoCaja turno,
        String key,
        String compradorNombre
    ) {
        var tipo = tiposComprobante
            .findByCodigo("INTERNO")
            .orElseThrow(() -> new ReglaNegocioException("No existe el tipo de comprobante INTERNO"));
        var serie = series
            .bloquear(new SerieComprobanteId(tipo.getIdTipoComprobante(), "RI01"))
            .orElseThrow(() -> new ReglaNegocioException("No existe la serie interna RI01"));
        String nombre =
            cliente == null
                ? (compradorNombre == null || compradorNombre.isBlank() ? "VENTA AL PÚBLICO" : compradorNombre.trim())
                : clientes.nombre(cliente.getPerfil());
        if (
            nombre.length() > 200 || (cliente != null && cliente.getPerfil().getNumeroDocumento().length() > 20)
        ) throw new ReglaNegocioException(
            "El comprobante interno admite hasta 20 caracteres de documento y 200 de nombre o razón social. Revisa los datos del cliente."
        );
        BigDecimal subtotal = cargos.stream().map(Consumo::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal igv = cargos.stream().map(Consumo::getIgv).reduce(BigDecimal.ZERO, BigDecimal::add);
        var recibo = new Comprobante();
        recibo.setCliente(cliente);
        recibo.setTipoComprobante(tipo);
        recibo.setTurnoCaja(turno);
        recibo.setSerie("RI01");
        recibo.setNumeroComprobante(String.format("%08d", serie.getSiguienteNumero()));
        serie.setSiguienteNumero(serie.getSiguienteNumero() + 1);
        recibo.setSubtotal(subtotal);
        recibo.setIgv(igv);
        recibo.setTotalPagar(subtotal.add(igv));
        recibo.setReceptorNombre(nombre);
        if (cliente != null) {
            recibo.setReceptorTipoDoc(cliente.getPerfil().getTipoDocumento().getCodigoSunat());
            recibo.setReceptorNumeroDoc(cliente.getPerfil().getNumeroDocumento());
        }
        recibo.setEstadoComprobante(EstadoComprobante.EMITIDO);
        recibo.setClaveIdempotencia(key);
        comprobanteRepository.save(recibo);
        int indice = 1;
        for (Consumo cargo : cargos) {
            var detalle = new ComprobanteDetalle();
            detalle.setComprobante(recibo);
            detalle.setConsumo(cargo);
            detalle.setNumeroLinea(indice++);
            detalle.setDescripcion(cargo.getDescripcion());
            detalle.setCantidad(cargo.getCantidad());
            detalle.setValorUnitario(cargo.getValorUnitario());
            detalle.setSubtotal(cargo.getSubtotal());
            detalle.setIgv(cargo.getIgv());
            detalle.setTotal(cargo.getTotal());
            if (cargo.getDetalleReserva() != null) detalle.setHabitacion(cargo.getDetalleReserva().getHabitacion());
            comprobanteDetalleRepository.save(detalle);
            cargo.setEstadoCargo(EstadoCargo.FACTURADO);
        }
        return recibo;
    }

    private Consumo cargo(DetalleReserva row) {
        return consumos
            .findByDetalleReservaIdDetalleReserva(row.getIdDetalleReserva())
            .stream()
            .filter(c -> c.getTipoConsumo() == TipoConsumo.HOSPEDAJE)
            .findFirst()
            .orElseThrow(() ->
                new ReglaNegocioException("La reserva no tiene cargo de hospedaje. Revisa sus datos antes de cobrar.")
            );
    }

    private BigDecimal total(List<DetalleReserva> rows) {
        return rows
            .stream()
            .flatMap(r -> consumos.findByDetalleReservaIdDetalleReserva(r.getIdDetalleReserva()).stream())
            .filter(c -> c.getEstadoCargo() != EstadoCargo.ANULADO)
            .map(Consumo::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String referencia(DetalleReserva reserva) {
        return "R" + reserva.getReserva().getIdReserva();
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo identificar la solicitud", ex);
        }
    }
}
