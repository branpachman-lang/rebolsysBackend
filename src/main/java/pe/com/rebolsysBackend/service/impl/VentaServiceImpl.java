package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.*;

import java.math.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
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
public class VentaServiceImpl implements VentaService {
  private final jakarta.persistence.EntityManager entityManager;
  private final VentaRepository ventas;
  private final ProductoRepository productos;
  private final PerfilRepository clientes;
  private final DetalleReservaRepository reservas;
  private final MovimientoInventarioRepository movimientos;
  private final ComprobanteRepository comprobantes;
  private final ReglasPropuesta reglas;
  private final CuentaReservaGestion cuentaReserva;
  private final ComprobanteDetalleRepository lineasCuenta;

  @Override
  @Transactional
  public VentaResponseDTO registrar(VentaRequestDTO r) {
    reglas.validar(r);
    if (r.idDetalleReserva() != null) bloquearReserva(r.idDetalleReserva());
    var usuario = reglas.bloquearUsuario(r.idUsuario());
    var firma = reglas.firma(r);
    var anterior = ventas.findByClaveRegistro(r.claveRegistro());
    if (anterior.isPresent()) {
      exigir(firma.equals(anterior.get().getFirmaSolicitud()),
          "La clave de registro ya corresponde a otra solicitud");
      return respuesta(anterior.get());
    }
    exigir(
        r.idClienteComprador() == null || limpiar(r.compradorNombreLibre()) == null, "Selecciona un comprador registrado o un nombre libre");
    exigir(
        r.modalidadCobro() != ModalidadCobro.RESERVA || r.idDetalleReserva() != null,
        "La modalidad RESERVA requiere una estancia");
    var v = new Venta();
    v.setUsuario(usuario);
    v.setClaveRegistro(r.claveRegistro());
    v.setFirmaSolicitud(firma);
    v.setModalidadCobro(r.modalidadCobro());
    v.setEstadoVenta(EstadoVenta.BORRADOR);
    v.setCompradorNombreLibre(limpiar(r.compradorNombreLibre()));
    if (r.idClienteComprador() != null)
      v.setClienteComprador(
          clientes
              .findByIdPerfilAndEsCliente(r.idClienteComprador(), 1)
              .orElseThrow(() -> new RecursoNoEncontradoException("El comprador no existe")));
    if (r.idDetalleReserva() != null) {
      var d =
          reservas
              .bloquear(r.idDetalleReserva())
              .orElseThrow(() -> new RecursoNoEncontradoException("La estancia no existe"));
      exigir(
          d.getEstadoDetalle() == EstadoReserva.EN_CURSO,
          "La venta a reserva requiere una estancia en curso");
      v.setDetalleReserva(d);
      cuentaReserva.validarVenta(v);
    }
    v.getDetalles().addAll(construirLineas(v, r.detalles()));
    ventas.saveAndFlush(v);
    return respuesta(v);
  }

  private List<DetalleVenta> construirLineas(Venta venta, List<VentaLineaRegistroDTO> solicitudes) {
    Set<Long> ids = new HashSet<>();
    List<DetalleVenta> resultado = new ArrayList<>();
    for (var l : solicitudes) {
      exigir(ids.add(l.idProducto()), "Agrupa las cantidades del mismo producto en una sola línea");
      var p =
          productos
              .findById(l.idProducto())
              .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe"));
      exigir(p.getEstadoProducto() == EstadoProducto.ACTIVO, "El producto está inactivo");
      exigir(l.precioEsperado()==null||l.precioEsperado().compareTo(p.getPrecioActual())==0,
          "El precio de "+p.getNombreProducto()+" cambió; actualiza el catálogo antes de confirmar");
      var d = new DetalleVenta();
      d.setVenta(venta);
      d.setProducto(p);
      d.setDescripcion(p.getNombreProducto());
      d.setCantidad(l.cantidad());
      d.setValorUnitario(p.getPrecioActual());
      d.setIgv(BigDecimal.ZERO);
      d.setEstadoLinea("ACTIVA");
      exigir(
          totalLinea(d).compareTo(new BigDecimal("9999999999.99")) <= 0,
          "El importe de la línea supera el máximo permitido");
      resultado.add(d);
    }
    exigir(resultado.stream().map(this::totalLinea).reduce(BigDecimal.ZERO, BigDecimal::add)
        .compareTo(new BigDecimal("9999999999.99")) <= 0, "El total de la venta supera el máximo permitido");
    return resultado;
  }

  @Override
  @Transactional
  public VentaResponseDTO registrarConfirmada(VentaRequestDTO r) {
    var venta = registrar(r);
    if (venta.estadoVenta() == EstadoVenta.BORRADOR) {
      var actuales = new HashMap<Long, Integer>();
      venta.detalles().forEach(d -> actuales.put(d.idProducto(), d.cantidad()));
      var solicitadas = new HashMap<Long, Integer>();
      r.detalles().forEach(d -> solicitadas.put(d.idProducto(), d.cantidad()));
      exigir(actuales.equals(solicitadas) && Objects.equals(venta.idClienteComprador(), r.idClienteComprador())
          && Objects.equals(venta.compradorNombreLibre(), limpiar(r.compradorNombreLibre())),
          "El borrador fue editado; consulta y confirma su contenido actual");
    }
    return confirmar(venta.idVenta(), new OperacionUsuarioDTO(r.idUsuario()));
  }

  @Override
  @Transactional
  public VentaResponseDTO editar(Long id, VentaEditarDTO r) {
    reglas.validar(r);
    reglas.usuario(r.idUsuario());
    var previo = ventas.findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("La venta no existe"));
    if (previo.getDetalleReserva() != null) bloquearReserva(previo.getDetalleReserva().getIdDetalleReserva());
    var v = bloquear(id);
    exigir(v.getEstadoVenta() == EstadoVenta.BORRADOR, "Solo puede editarse una venta en borrador");
    exigir(r.idClienteComprador() == null || limpiar(r.compradorNombreLibre()) == null,
        "Selecciona un comprador registrado o un nombre libre");
    cuentaReserva.validarVenta(v);
    var nuevas = construirLineas(v, r.detalles());
    var comprador = r.idClienteComprador() == null ? null : clientes.findByIdPerfilAndEsCliente(r.idClienteComprador(), 1)
        .orElseThrow(() -> new RecursoNoEncontradoException("El comprador no existe"));
    v.setClienteComprador(comprador);
    v.setCompradorNombreLibre(limpiar(r.compradorNombreLibre()));
    v.getDetalles().clear();
    v.getDetalles().addAll(nuevas);
    ventas.saveAndFlush(v);
    return respuesta(v);
  }

  @Override
  @Transactional
  public VentaResponseDTO confirmar(Long id, OperacionUsuarioDTO r) {
    reglas.validar(r);
    var previo = ventas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("La venta no existe"));
    if (previo.getDetalleReserva() != null)
      bloquearReserva(previo.getDetalleReserva().getIdDetalleReserva());
    var operador = reglas.usuario(r.idUsuario());
    var v = bloquear(id);
    if (v.getEstadoVenta() == EstadoVenta.CONFIRMADA) {
      cuentaReserva.registrarVenta(v, r.idUsuario());
      return respuesta(v);
    }
    exigir(
        v.getEstadoVenta() == EstadoVenta.BORRADOR, "Solo puede confirmarse una venta en borrador");
    cuentaReserva.turnoOperador(r.idUsuario());
    if (v.getDetalleReserva() != null) {
      var d = reservas.bloquear(v.getDetalleReserva().getIdDetalleReserva()).orElseThrow();
      entityManager.refresh(d);
      exigir(
          d.getEstadoDetalle() == EstadoReserva.EN_CURSO,
          "La estancia debe continuar en curso al confirmar");
      cuentaReserva.validarVenta(v);
    }
    var bloqueados = bloquearProductos(v);
    var tipo = reglas.tipo("VENTA", -1);
    for (var d : v.getDetalles()) {
      var p = bloqueados.get(d.getProducto().getIdProducto());
      exigir(
          p.getEstadoProducto() == EstadoProducto.ACTIVO, "Un producto de la venta está inactivo");
      exigir(p.getStock() >= d.getCantidad(), "Stock insuficiente para " + p.getNombreProducto());
    }
    for (var d : v.getDetalles()) {
      var p = bloqueados.get(d.getProducto().getIdProducto());
      p.setStock(p.getStock() - d.getCantidad());
      movimiento(v, d, p, operador, tipo, "Confirmación de venta", "venta:");
    }
    v.setEstadoVenta(EstadoVenta.CONFIRMADA);
    v.setFechaConfirmacion(ahora());
    ventas.flush();
    cuentaReserva.registrarVenta(v, r.idUsuario());
    return respuesta(v);
  }

  @Override
  @Transactional
  public VentaResponseDTO anular(Long id, OperacionUsuarioDTO r) {
    reglas.validar(r);
    var previo =
        ventas
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("La venta no existe"));
    if (previo.getDetalleReserva() != null)
      bloquearReserva(previo.getDetalleReserva().getIdDetalleReserva());
    var operador = reglas.usuario(r.idUsuario());
    var v = bloquear(id);
    if (v.getEstadoVenta() == EstadoVenta.ANULADA) return respuesta(v);
    exigir(
        !comprobantes.existsByVentaIdVenta(id)
            && !lineasCuenta.existsByDetalleVentaVentaIdVenta(id),
        "La venta tiene comprobantes asociados y requiere el flujo de anulación de comprobantes");
    if (v.getEstadoVenta() == EstadoVenta.CONFIRMADA) {
      cuentaReserva.turnoOperador(r.idUsuario());
      var bloqueados = bloquearProductos(v);
      var tipo = reglas.tipo("DEV_VENTA", 1);
      for (var d : v.getDetalles()) {
        var p = bloqueados.get(d.getProducto().getIdProducto());
        long nuevo = (long) p.getStock() + d.getCantidad();
        exigir(nuevo <= 999999999, "El stock resultante supera el máximo permitido");
        p.setStock((int) nuevo);
        movimiento(v, d, p, operador, tipo, "Anulación de venta", "anular:");
      }
    }
    for (var d : v.getDetalles()) d.setEstadoLinea("ANULADA");
    v.setEstadoVenta(EstadoVenta.ANULADA);
    ventas.flush();
    return respuesta(v);
  }

  private Map<Long, Producto> bloquearProductos(Venta v) {
    Map<Long, Producto> resultado = new HashMap<>();
    for (Long id :
        v.getDetalles().stream()
            .map(d -> d.getProducto().getIdProducto())
            .distinct()
            .sorted()
            .toList()) {
      var p =
          productos
              .bloquear(id)
              .orElseThrow(() -> new RecursoNoEncontradoException("El producto no existe"));
      entityManager.refresh(p);
      resultado.put(id, p);
    }
    return resultado;
  }

  private void bloquearReserva(Long idDetalle) {
    var d =
        reservas
            .findById(idDetalle)
            .orElseThrow(() -> new RecursoNoEncontradoException("La estancia no existe"));
    cuentaReserva.bloquear(d.getReserva().getIdReserva());
  }

  private void movimiento(
      Venta v,
      DetalleVenta d,
      Producto p,
      Usuario u,
      TipoMovimientoAlmacen t,
      String texto,
      String prefijo) {
    var m = new MovimientoInventario();
    m.setProducto(p);
    m.setDetalleVenta(d);
    m.setUsuario(u);
    m.setTipoMovimientoAlmacen(t);
    m.setCantidad(d.getCantidad());
    m.setStockResultante(p.getStock());
    m.setClaveOperacion(prefijo + d.getIdDetalleVenta());
    m.setObservacion(texto + " " + v.getIdVenta());
    movimientos.save(m);
  }

  private Venta bloquear(Long id) {
    var v =
        ventas
            .bloquear(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("La venta no existe"));
    entityManager.refresh(v);
    return v;
  }

  @Override
  @Transactional(readOnly = true)
  public VentaResponseDTO obtener(Long id) {
    return respuesta(
        ventas
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("La venta no existe")));
  }

  @Override
  @Transactional(readOnly = true)
  public PaginaResponseDTO<VentaResponseDTO> listar(int pagina, int tamanio) {
    var p = ventas.findAll(PageRequest.of(pagina, tamanio, Sort.by("idVenta").descending()));
    return new PaginaResponseDTO<>(
        p.getContent().stream().map(this::respuesta).toList(),
        pagina,
        tamanio,
        p.getTotalElements(),
        p.getTotalPages(),
        p.isLast());
  }

  private BigDecimal totalLinea(DetalleVenta d) {
    return d.getValorUnitario()
        .multiply(BigDecimal.valueOf(d.getCantidad()))
        .setScale(2, RoundingMode.HALF_UP)
        .add(d.getIgv());
  }

  private VentaResponseDTO respuesta(Venta v) {
    var lineas =
        v.getDetalles().stream()
            .map(
                d -> {
                  var sub = totalLinea(d).subtract(d.getIgv());
                  return new VentaLineaRespuestaDTO(
                      d.getIdDetalleVenta(),
                      d.getProducto().getIdProducto(),
                      d.getDescripcion(),
                      d.getCantidad(),
                      d.getValorUnitario(),
                      d.getIgv(),
                      sub,
                      totalLinea(d),
                      d.getEstadoLinea());
                })
            .toList();
    return new VentaResponseDTO(
        v.getIdVenta(),
        v.getClienteComprador() == null ? null : v.getClienteComprador().getIdPerfil(),
        v.getCompradorNombreLibre(),
        v.getDetalleReserva() == null ? null : v.getDetalleReserva().getIdDetalleReserva(),
        v.getUsuario().getIdUsuario(),
        v.getModalidadCobro(),
        v.getEstadoVenta(),
        v.getFechaRegistro(),
        v.getFechaConfirmacion(),
        v.getClaveRegistro(),
        lineas.stream()
            .filter(d -> "ACTIVA".equals(d.estadoLinea()))
            .map(VentaLineaRespuestaDTO::total)
            .reduce(BigDecimal.ZERO, BigDecimal::add),
        lineas);
  }
}
