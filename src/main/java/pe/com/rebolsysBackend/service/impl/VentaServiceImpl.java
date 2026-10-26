package pe.com.rebolsysBackend.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.PaginaResponseDTO;
import pe.com.rebolsysBackend.dto.ReservaVentaResponseDTO;
import pe.com.rebolsysBackend.dto.VentaActualizarRequestDTO;
import pe.com.rebolsysBackend.dto.VentaDetalleRequestDTO;
import pe.com.rebolsysBackend.dto.VentaDetalleResponseDTO;
import pe.com.rebolsysBackend.dto.VentaRequestDTO;
import pe.com.rebolsysBackend.dto.VentaResponseDTO;
import pe.com.rebolsysBackend.entity.Cliente;
import pe.com.rebolsysBackend.entity.Comprobante;
import pe.com.rebolsysBackend.entity.Consumo;
import pe.com.rebolsysBackend.entity.DetalleReserva;
import pe.com.rebolsysBackend.entity.MovimientoInventario;
import pe.com.rebolsysBackend.entity.Producto;
import pe.com.rebolsysBackend.entity.Venta;
import pe.com.rebolsysBackend.enums.EstadoCargo;
import pe.com.rebolsysBackend.enums.EstadoCobro;
import pe.com.rebolsysBackend.enums.EstadoProducto;
import pe.com.rebolsysBackend.enums.EstadoReserva;
import pe.com.rebolsysBackend.enums.ModoVenta;
import pe.com.rebolsysBackend.enums.TipoConsumo;
import pe.com.rebolsysBackend.exception.RecursoNoEncontradoException;
import pe.com.rebolsysBackend.exception.ReglaNegocioException;
import pe.com.rebolsysBackend.repository.ClienteRepository;
import pe.com.rebolsysBackend.repository.ComprobanteRepository;
import pe.com.rebolsysBackend.repository.ConsumoRepository;
import pe.com.rebolsysBackend.repository.DetalleReservaRepository;
import pe.com.rebolsysBackend.repository.MovimientoInventarioRepository;
import pe.com.rebolsysBackend.repository.ProductoRepository;
import pe.com.rebolsysBackend.repository.ReservaHuespedRepository;
import pe.com.rebolsysBackend.repository.ReservaRepository;
import pe.com.rebolsysBackend.repository.TipoMovimientoAlmacenRepository;
import pe.com.rebolsysBackend.repository.UsuarioRepository;
import pe.com.rebolsysBackend.repository.VentaRepository;
import pe.com.rebolsysBackend.service.service.ClienteService;
import pe.com.rebolsysBackend.service.service.ComprobanteService;
import pe.com.rebolsysBackend.service.service.PagoService;
import pe.com.rebolsysBackend.service.service.ReservaService;
import pe.com.rebolsysBackend.service.service.VentaService;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productos;
    private final ConsumoRepository consumos;
    private final MovimientoInventarioRepository movimientos;
    private final TipoMovimientoAlmacenRepository tiposMovimiento;
    private final DetalleReservaRepository detalleReservaRepository;
    private final ReservaRepository reservaRepository;
    private final ClienteRepository clientes;
    private final UsuarioRepository usuarios;
    private final ReservaHuespedRepository huespedes;
    private final ClienteService clienteService;
    private final PagoService pagoService;
    private final ComprobanteService comprobanteService;
    private final ComprobanteRepository comprobantes;
    private final ReservaService reservaService;
    private final EntityManager em;
    private final Validator validator;
    private final ObjectMapper json;

    @Override
    @Transactional
    public VentaResponseDTO registrar(VentaRequestDTO request) {
        var errores = validator.validate(request);
        if (!errores.isEmpty()) throw new ConstraintViolationException(errores);
        String firma = hash(json.writeValueAsString(request));
        var anterior = ventaRepository.findByClaveRegistro(request.getClaveRegistro());
        if (anterior.isPresent()) return reintento(anterior.get(), firma, request.getEfectivoRecibido());
        if (
            request.getDetalles().stream().map(VentaDetalleRequestDTO::getIdProducto).distinct().count() !=
            request.getDetalles().size()
        ) throw new ReglaNegocioException("Agrupa la cantidad de cada producto en una sola línea del carrito");
        DetalleReserva reserva = null;
        Cliente cliente = null;
        if (request.getModoVenta() == ModoVenta.CARGO_RESERVA) {
            reserva = detalleReservaRepository
                .findById(request.getIdDetalleReserva())
                .orElseThrow(() -> new RecursoNoEncontradoException("La reserva no existe"));
            reservaRepository.bloquear(reserva.getReserva().getIdReserva()).orElseThrow();
            em.refresh(reserva, LockModeType.PESSIMISTIC_WRITE);
            if (
                reserva.getEstadoDetalle() != EstadoReserva.CONFIRMADA &&
                reserva.getEstadoDetalle() != EstadoReserva.EN_CURSO
            ) throw new ReglaNegocioException("Solo se pueden cargar productos a una reserva confirmada o en curso");
            cliente = reserva.getReserva().getCliente();
            if (
                request.getIdCliente() != null && !request.getIdCliente().equals(cliente.getIdCliente())
            ) throw new ReglaNegocioException("El cargo corresponde al titular de la reserva seleccionada");
        } else if (request.getIdCliente() != null) cliente = clientes
            .findById(request.getIdCliente())
            .orElseThrow(() -> new RecursoNoEncontradoException("El cliente no existe"));
        Map<Long, Producto> seleccionados = new LinkedHashMap<>();
        request
            .getDetalles()
            .stream()
            .map(VentaDetalleRequestDTO::getIdProducto)
            .sorted()
            .forEach(id ->
                seleccionados.put(
                    id,
                    productos.bloquear(id).orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe"))
                )
            );
        anterior = ventaRepository.findByClaveRegistro(request.getClaveRegistro());
        if (anterior.isPresent()) return reintento(anterior.get(), firma, request.getEfectivoRecibido());
        BigDecimal total = BigDecimal.ZERO;
        for (var d : request.getDetalles()) {
            Producto p = seleccionados.get(d.getIdProducto());
            if (
                d.getPrecioEsperado() != null && d.getPrecioEsperado().compareTo(p.getPrecioActual()) != 0
            ) throw new ReglaNegocioException(
                "El precio de " + p.getNombreProducto() + " cambió. Recarga el catálogo antes de confirmar la venta"
            );
            if (p.getEstadoProducto() != EstadoProducto.ACTIVO) throw new ReglaNegocioException(
                "El producto " + p.getNombreProducto() + " está inactivo"
            );
            if (p.getStock() < d.getCantidad()) throw new ReglaNegocioException(
                "Stock insuficiente para " + p.getNombreProducto() + ". Recarga las existencias disponibles"
            );
            total = total.add(p.getPrecioActual().multiply(BigDecimal.valueOf(d.getCantidad())));
        }
        if (total.compareTo(new BigDecimal("99999999.99")) > 0) throw new ReglaNegocioException(
            "El total de la venta excede el importe admitido"
        );
        if (
            reserva != null &&
            reservaService.read(referencia(reserva)).getTotal().add(total).compareTo(new BigDecimal("99999999.99")) > 0
        ) throw new ReglaNegocioException("El total de la reserva con sus consumos excede el importe admitido");
        var actor = usuarios
            .findByUsername("operacion_local")
            .orElseThrow(() ->
                new RecursoNoEncontradoException("No existe el usuario responsable de registrar la venta")
            );
        var salida = tiposMovimiento
            .findByCodigo("SAL_VENTA")
            .orElseThrow(() -> new ReglaNegocioException("Falta el tipo de salida por venta en almacén"));
        if (salida.getSigno() != -1) throw new ReglaNegocioException(
            "La venta requiere un movimiento de salida de almacén"
        );
        var venta = new Venta();
        venta.setClaveRegistro(request.getClaveRegistro());
        venta.setFirmaSolicitud(firma);
        venta.setModoVenta(request.getModoVenta());
        venta.setDetalleReserva(reserva);
        venta.setCliente(cliente);
        venta.setUsuario(actor);
        venta.setCompradorNombre(
            request.getCompradorNombre() == null || request.getCompradorNombre().isBlank()
                ? (cliente == null ? "VENTA AL PÚBLICO" : clienteService.nombre(cliente.getPerfil()))
                : request.getCompradorNombre().trim()
        );
        ventaRepository.saveAndFlush(venta);
        List<Consumo> cargos = new ArrayList<>();
        for (var d : request.getDetalles()) {
            var p = seleccionados.get(d.getIdProducto());
            p.setStock(p.getStock() - d.getCantidad());
            var c = new Consumo();
            c.setVenta(venta);
            c.setCliente(cliente);
            c.setDetalleReserva(reserva);
            c.setProducto(p);
            c.setUsuario(actor);
            c.setTipoConsumo(TipoConsumo.PRODUCTO);
            c.setDescripcion(p.getNombreProducto());
            c.setCantidad(BigDecimal.valueOf(d.getCantidad()));
            c.setValorUnitario(p.getPrecioActual());
            c.setSubtotal(p.getPrecioActual().multiply(c.getCantidad()));
            c.setIgv(BigDecimal.ZERO);
            c.setTotal(c.getSubtotal());
            c.setEstadoCargo(EstadoCargo.PENDIENTE);
            c.setEstadoCobro(EstadoCobro.PENDIENTE);
            consumos.saveAndFlush(c);
            cargos.add(c);
            var m = new MovimientoInventario();
            m.setProducto(p);
            m.setIdConsumo(c.getIdConsumo());
            m.setConsumo(c);
            m.setTipoMovimientoAlmacen(salida);
            m.setCantidad(d.getCantidad());
            m.setStockResultante(p.getStock());
            m.setUsuario(actor);
            m.setObservacion("Venta " + venta.getIdVenta());
            movimientos.save(m);
        }
        BigDecimal vuelto = BigDecimal.ZERO;
        if (venta.getModoVenta() == ModoVenta.SEPARADA) {
            var recibo = pagoService.cobrarVenta(
                cargos,
                cliente,
                actor,
                venta.getCompradorNombre(),
                request.getClaveRegistro(),
                request.getEfectivoRecibido()
            );
            venta.setComprobante(recibo);
            vuelto = request.getEfectivoRecibido().subtract(total);
        }
        em.flush();
        return convertirResponse(venta, vuelto);
    }

    private VentaResponseDTO reintento(Venta venta, String firma, BigDecimal efectivoRecibido) {
        if (!firma.equals(venta.getFirmaSolicitud())) throw new ReglaNegocioException(
            "La clave de venta ya fue usada con otros datos. Revisa el historial antes de volver a registrar"
        );
        var respuesta = convertirResponse(venta, BigDecimal.ZERO);
        if (venta.getModoVenta() == ModoVenta.SEPARADA) {
            respuesta.setVuelto(efectivoRecibido.subtract(respuesta.getTotal()));
        }
        return respuesta;
    }

    @Override
    @Transactional
    public VentaResponseDTO actualizar(Long id, VentaActualizarRequestDTO request) {
        var errores = validator.validate(request);
        if (!errores.isEmpty()) throw new ConstraintViolationException(errores);
        var previa = ventaRepository
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("La venta no existe"));
        if (previa.getDetalleReserva() != null) reservaRepository
            .bloquear(previa.getDetalleReserva().getReserva().getIdReserva())
            .orElseThrow();
        var venta = ventaRepository
            .bloquear(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("La venta no existe"));
        em.refresh(venta, LockModeType.PESSIMISTIC_WRITE);
        var actuales = consumos
            .findByVentaIdVentaOrderByIdConsumo(id)
            .stream()
            .filter(c -> c.getEstadoCargo() != EstadoCargo.ANULADO)
            .toList();
        if (!esEditable(venta, actuales, comprobantes.buscarPorVenta(id))) throw new ReglaNegocioException(
            "Solo se pueden editar ventas pendientes, sin cobros ni comprobante y con una reserva activa"
        );
        if (
            request.getDetalles().stream().map(VentaDetalleRequestDTO::getIdProducto).distinct().count() !=
            request.getDetalles().size()
        ) throw new ReglaNegocioException("Agrupa la cantidad de cada producto en una sola línea del carrito");
        Map<Long, Consumo> anteriores = new LinkedHashMap<>();
        actuales.forEach(c -> anteriores.put(c.getProducto().getIdProducto(), c));
        Map<Long, VentaDetalleRequestDTO> nuevas = new LinkedHashMap<>();
        request.getDetalles().forEach(d -> nuevas.put(d.getIdProducto(), d));
        var ids = new java.util.TreeSet<Long>(anteriores.keySet());
        ids.addAll(nuevas.keySet());
        Map<Long, Producto> bloqueados = new LinkedHashMap<>();
        for (Long productoId : ids)
            bloqueados.put(
                productoId,
                productos
                    .bloquear(productoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe"))
            );
        BigDecimal totalNuevo = BigDecimal.ZERO;
        for (var d : request.getDetalles()) {
            var anterior = anteriores.get(d.getIdProducto());
            var producto = bloqueados.get(d.getIdProducto());
            int cantidadAnterior = anterior == null ? 0 : anterior.getCantidad().intValueExact();
            if (
                d.getCantidad() > cantidadAnterior && producto.getEstadoProducto() != EstadoProducto.ACTIVO
            ) throw new ReglaNegocioException("El producto " + producto.getNombreProducto() + " está inactivo");
            BigDecimal precio = anterior == null ? producto.getPrecioActual() : anterior.getValorUnitario();
            if (
                d.getPrecioEsperado() != null && d.getPrecioEsperado().compareTo(precio) != 0
            ) throw new ReglaNegocioException("El precio del producto cambió. Recarga la venta antes de guardar");
            totalNuevo = totalNuevo.add(precio.multiply(BigDecimal.valueOf(d.getCantidad())));
        }
        var totalAnterior = actuales.stream().map(Consumo::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (
            totalNuevo.compareTo(new BigDecimal("99999999.99")) > 0 ||
            reservaService
                .read(referencia(venta.getDetalleReserva()))
                .getTotal()
                .subtract(totalAnterior)
                .add(totalNuevo)
                .compareTo(new BigDecimal("99999999.99")) >
            0
        ) throw new ReglaNegocioException("El total de la venta o reserva excede el importe admitido");
        for (Long productoId : ids) {
            var producto = bloqueados.get(productoId);
            var anterior = anteriores.get(productoId);
            var nueva = nuevas.get(productoId);
            int cantidadAnterior = anterior == null ? 0 : anterior.getCantidad().intValueExact();
            int cantidadNueva = nueva == null ? 0 : nueva.getCantidad();
            long stock = (long) producto.getStock() + cantidadAnterior - cantidadNueva;
            if (stock < 0) throw new ReglaNegocioException("Stock insuficiente para " + producto.getNombreProducto());
            if (stock > 999999999) throw new ReglaNegocioException("El stock resultante excede el límite admitido");
            int diferencia = cantidadNueva - cantidadAnterior;
            Consumo cargo = anterior;
            if (nueva != null) {
                if (cargo == null) {
                    cargo = new Consumo();
                    cargo.setVenta(venta);
                    cargo.setProducto(producto);
                    cargo.setCliente(venta.getCliente());
                    cargo.setDetalleReserva(venta.getDetalleReserva());
                    cargo.setUsuario(venta.getUsuario());
                    cargo.setTipoConsumo(TipoConsumo.PRODUCTO);
                    cargo.setDescripcion(producto.getNombreProducto());
                    cargo.setValorUnitario(producto.getPrecioActual());
                    cargo.setEstadoCargo(EstadoCargo.PENDIENTE);
                    cargo.setEstadoCobro(EstadoCobro.PENDIENTE);
                }
                cargo.setCantidad(BigDecimal.valueOf(cantidadNueva));
                cargo.setSubtotal(cargo.getValorUnitario().multiply(cargo.getCantidad()));
                cargo.setIgv(BigDecimal.ZERO);
                cargo.setTotal(cargo.getSubtotal());
                consumos.saveAndFlush(cargo);
            } else {
                cargo.setEstadoCargo(EstadoCargo.ANULADO);
                cargo.setEstadoCobro(EstadoCobro.ANULADO);
            }
            producto.setStock((int) stock);
            if (diferencia != 0) registrarAjusteEdicion(venta, cargo, producto, diferencia);
        }
        venta.setCompradorNombre(
            request.getCompradorNombre() == null || request.getCompradorNombre().isBlank()
                ? clienteService.nombre(venta.getCliente().getPerfil())
                : request.getCompradorNombre().trim()
        );
        em.flush();
        return convertirResponse(venta, BigDecimal.ZERO);
    }

    private void registrarAjusteEdicion(Venta venta, Consumo cargo, Producto producto, int diferencia) {
        String codigo = diferencia > 0 ? "SAL_VENTA" : "AJ_ENT";
        var tipo = tiposMovimiento
            .findByCodigo(codigo)
            .orElseThrow(() -> new ReglaNegocioException("Falta el tipo de ajuste de almacén"));
        if (tipo.getSigno() != (diferencia > 0 ? -1 : 1)) throw new ReglaNegocioException(
            "El tipo de movimiento de almacén no corresponde al ajuste"
        );
        var movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        // El enlace único idConsumo conserva la salida original; los ajustes mantienen su referencia en el motivo.
        movimiento.setTipoMovimientoAlmacen(tipo);
        movimiento.setCantidad(Math.abs(diferencia));
        movimiento.setStockResultante(producto.getStock());
        movimiento.setUsuario(venta.getUsuario());
        movimiento.setObservacion("Edición de venta " + venta.getIdVenta() + ", consumo " + cargo.getIdConsumo());
        movimientos.save(movimiento);
    }

    private boolean esEditable(Venta venta, List<Consumo> cargos, List<Comprobante> recibos) {
        return (
            venta.getModoVenta() == ModoVenta.CARGO_RESERVA &&
            venta.getComprobante() == null &&
            recibos.isEmpty() &&
            !cargos.isEmpty() &&
            cargos
                .stream()
                .allMatch(
                    c -> c.getEstadoCargo() == EstadoCargo.PENDIENTE && c.getEstadoCobro() == EstadoCobro.PENDIENTE
                ) &&
            venta.getDetalleReserva() != null &&
            (venta.getDetalleReserva().getEstadoDetalle() == EstadoReserva.CONFIRMADA ||
                venta.getDetalleReserva().getEstadoDetalle() == EstadoReserva.EN_CURSO)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponseDTO buscar(Long id) {
        return convertirResponse(
            ventaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("La venta no existe")),
            BigDecimal.ZERO
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponseDTO<VentaResponseDTO> listar(int pagina, int tamanio) {
        var p = ventaRepository.findAll(PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "idVenta")));
        return new PaginaResponseDTO<>(
            p
                .getContent()
                .stream()
                .map(v -> convertirResponse(v, BigDecimal.ZERO))
                .toList(),
            pagina,
            tamanio,
            p.getTotalElements(),
            p.getTotalPages(),
            p.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaVentaResponseDTO> buscarReservas(String buscar) {
        String texto = buscar == null ? "" : buscar.trim().toLowerCase(Locale.ROOT);
        return detalleReservaRepository
            .findAllByOrderByIdDetalleReservaDesc()
            .stream()
            .filter(
                r -> r.getEstadoDetalle() == EstadoReserva.CONFIRMADA || r.getEstadoDetalle() == EstadoReserva.EN_CURSO
            )
            .map(r ->
                new ReservaVentaResponseDTO(
                    r.getIdDetalleReserva(),
                    r.getReserva().getIdReserva(),
                    referencia(r),
                    String.format("%07d", r.getReserva().getIdReserva()),
                    r.getHabitacion().getNumeroCuarto(),
                    clienteService.nombre(r.getReserva().getCliente().getPerfil()),
                    r.getReserva().getCliente().getIdCliente(),
                    huespedes
                        .findByDetalleReservaIdDetalleReserva(r.getIdDetalleReserva())
                        .stream()
                        .map(h -> clienteService.nombre(h.getHuesped().getPersonaNatural().getPerfil()))
                        .toList()
                )
            )
            .filter(r ->
                (
                    r.getHabitacion() +
                    " " +
                    r.getNumero() +
                    " " +
                    r.getClienteNombre() +
                    " " +
                    r.getHuespedes()
                ).toLowerCase(Locale.ROOT).contains(texto)
            )
            .limit(100)
            .toList();
    }

    private VentaResponseDTO convertirResponse(Venta v, BigDecimal vuelto) {
        var cargos = consumos
            .findByVentaIdVentaOrderByIdConsumo(v.getIdVenta())
            .stream()
            .filter(c -> c.getEstadoCargo() != EstadoCargo.ANULADO)
            .toList();
        BigDecimal subtotal = cargos.stream().map(Consumo::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add),
            igv = cargos.stream().map(Consumo::getIgv).reduce(BigDecimal.ZERO, BigDecimal::add);
        var recibos = comprobantes
            .buscarPorVenta(v.getIdVenta())
            .stream()
            .sorted(java.util.Comparator.comparing(Comprobante::getIdComprobante))
            .toList();
        Comprobante recibo =
            v.getComprobante() != null ? v.getComprobante() : (recibos.isEmpty() ? null : recibos.getLast());
        String estado = cargos.stream().allMatch(c -> c.getEstadoCobro() == EstadoCobro.ANULADO)
            ? "ANULADO"
            : cargos.stream().allMatch(c -> c.getEstadoCobro() == EstadoCobro.PAGADO)
                ? "PAGADO"
                : "PENDIENTE";
        return new VentaResponseDTO(
            v.getIdVenta(),
            String.format("%07d", v.getIdVenta()),
            v.getFechaVenta(),
            v.getModoVenta(),
            v.getCompradorNombre(),
            v.getDetalleReserva() == null ? null : v.getDetalleReserva().getIdDetalleReserva(),
            v.getDetalleReserva() == null ? null : v.getDetalleReserva().getReserva().getIdReserva(),
            v.getDetalleReserva() == null ? null : referencia(v.getDetalleReserva()),
            v.getDetalleReserva() == null ? null : v.getDetalleReserva().getHabitacion().getNumeroCuarto(),
            estado,
            subtotal,
            igv,
            subtotal.add(igv),
            vuelto,
            cargos
                .stream()
                .map(c ->
                    new VentaDetalleResponseDTO(
                        c.getIdConsumo(),
                        c.getProducto().getIdProducto(),
                        c.getDescripcion(),
                        c.getCantidad().intValueExact(),
                        c.getValorUnitario(),
                        c.getTotal(),
                        c.getProducto().getStock(),
                        c.getProducto().getImagenRuta()
                    )
                )
                .toList(),
            recibo == null ? null : comprobanteService.obtenerPorId(recibo.getIdComprobante()),
            esEditable(v, cargos, recibos)
        );
    }

    private String referencia(DetalleReserva r) {
        return "R" + r.getReserva().getIdReserva();
    }

    private String hash(String valor) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo identificar la venta", ex);
        }
    }
}
