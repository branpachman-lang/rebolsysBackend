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
public class ItemServiceImpl implements ItemService {
  private final jakarta.persistence.EntityManager entityManager;
  private final ItemRepository items;
  private final TipoItemRepository tipos;
  private final DetalleHabitacionRepository detalles;
  private final HabitacionRepository habitaciones;
  private final MovimientoInventarioRepository movimientos;
  private final ReglasPropuesta reglas;
  private final InformeRevisionRepository informes;

  @Override
  @Transactional
  public ItemResponseDTO create(ItemRequestDTO r) {
    return guardar(null, r);
  }

  @Override
  @Transactional
  public ItemResponseDTO update(Long id, ItemRequestDTO r) {
    Objects.requireNonNull(id, "El identificador es obligatorio");
    return guardar(id, r);
  }

  @Override
  @Transactional(readOnly = true)
  public ItemResponseDTO read(Long id) {
    return obtener(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ItemResponseDTO> readAll() {
    return listar();
  }

  @Override
  @Transactional
  public void delete(Long id) {
    var i = items.bloquear(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("El ítem no existe"));
    entityManager.refresh(i);
    exigir(i.getEstadoItem() != EstadoItem.EN_HABITACION,
        "Retira el ítem de la habitación antes de desactivarlo");
    i.setEstadoItem(EstadoItem.INACTIVO);
    items.saveAndFlush(i);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ItemResponseDTO> listar() {
    return items.findAll(Sort.by("nombreItem")).stream().map(this::respuesta).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public PaginaResponseDTO<ItemResponseDTO> configuracion(ItemFiltroDTO f) {
    reglas.validar(f);
    if (f.idTipoItem() != null && !tipos.existsById(f.idTipoItem()))
      throw new RecursoNoEncontradoException("El tipo de ítem no existe");
    if (f.idHabitacion() != null && !habitaciones.existsById(f.idHabitacion()))
      throw new RecursoNoEncontradoException("La habitación no existe");
    var page = items.buscarConfiguracion(patronBusqueda(f.buscar()), f.idTipoItem(), f.estado(),
        f.idHabitacion(), f.sinAsignar(), f.estadoConservacion(),
        PageRequest.of(f.pagina(), f.tamanio(), Sort.by("nombreItem", "idItem")));
    return new PaginaResponseDTO<>(page.map(this::respuesta).getContent(), f.pagina(), f.tamanio(),
        page.getTotalElements(), page.getTotalPages(), page.isLast());
  }

  @Override
  @Transactional(readOnly = true)
  public List<ItemResponseDTO> enHabitacion(Long id) {
    if (!habitaciones.existsById(id)) throw new RecursoNoEncontradoException("La habitación no existe");
    return items.actualesHabitacion(id).stream().map(this::respuesta).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public ItemResponseDTO obtener(Long id) {
    return respuesta(
        items
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("El ítem no existe")));
  }

  @Override
  @Transactional
  public ItemResponseDTO guardar(Long id, ItemRequestDTO r) {
    reglas.validar(r);
    var i =
        id == null
            ? new Item()
            : items
                .bloquear(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El ítem no existe"));
    if (id != null) entityManager.refresh(i);
    if (id != null && i.getEstadoItem() != EstadoItem.INACTIVO && r.estadoItem() == EstadoItem.INACTIVO) {
      SesionOperacionService.exigirPermisoEnPeticion("MOBILIARIO:ELIMINAR");
    }
    boolean asignado =
        id != null
            && !detalles.findByItemIdItem(id).isEmpty()
            && i.getEstadoItem() == EstadoItem.EN_HABITACION;
    exigir(
        (r.estadoItem() == EstadoItem.EN_HABITACION) == asignado,
        "Asigna o retira el ítem desde la operación de asignación");
    i.setNombreItem(r.nombreItem().trim());
    i.setTipoItem(
        tipos
            .findById(r.idTipoItem())
            .orElseThrow(() -> new RecursoNoEncontradoException("El tipo de ítem no existe")));
    i.setEstadoItem(r.estadoItem());
    return respuesta(items.saveAndFlush(i));
  }

  @Override
  @Transactional
  public ItemResponseDTO asignar(Long id, ItemAsignacionDTO r) {
    reglas.validar(r);
    var u = reglas.usuario(r.idUsuario());
    var i =
        items.bloquear(id).orElseThrow(() -> new RecursoNoEncontradoException("El ítem no existe"));
    entityManager.refresh(i);
    String clave = hash("item:" + r.claveOperacion());
    var previo = movimientos.findByClaveOperacion(clave);
    if (previo.isPresent()) {
      var m = previo.get();
      exigir(
          m.getItem() != null
              && m.getItem().getIdItem().equals(id)
              && m.getObservacion().endsWith("[" + reglas.firma(r) + "]"),
          "La clave de operación corresponde a otra asignación");
      return respuesta(i);
    }
    exigir(i.getEstadoItem() != EstadoItem.INACTIVO, "Activa el ítem antes de asignarlo");
    var d = asignacionActual(i);
    var habitacionesAfectadas = new TreeSet<Long>();
    if (i.getEstadoItem() == EstadoItem.EN_HABITACION && d != null) {
      habitacionesAfectadas.add(d.getHabitacion().getIdHabitacion());
    }
    if (r.idHabitacion() != null) habitacionesAfectadas.add(r.idHabitacion());
    habitacionesAfectadas.forEach(this::bloquearSinLimpieza);
    if (r.idHabitacion() != null) {
      var h = habitaciones.getReferenceById(r.idHabitacion());
      d = new DetalleHabitacion();
      d.setItem(i);
      d.setHabitacion(h);
      d.setEstadoActualItem(r.estadoActualItem());
      detalles.saveAndFlush(d);
      i.setEstadoItem(EstadoItem.EN_HABITACION);
    } else {
      exigir(
          i.getEstadoItem() == EstadoItem.EN_HABITACION && d != null,
          "El ítem no está asignado a una habitación");
      var retiro = new DetalleHabitacion();
      retiro.setItem(i);
      retiro.setHabitacion(d.getHabitacion());
      retiro.setEstadoActualItem(r.estadoActualItem());
      d = detalles.saveAndFlush(retiro);
      i.setEstadoItem(EstadoItem.ACTIVO);
    }
    var m = new MovimientoInventario();
    m.setItem(i);
    m.setDetalleHabitacion(d);
    m.setUsuario(u);
    m.setTipoMovimientoAlmacen(reglas.tipo(r.idHabitacion() == null ? "RET_ITEM" : "ASIG_ITEM", 0));
    m.setCantidad(1);
    m.setStockResultante(r.idHabitacion() == null ? 1 : 0);
    m.setClaveOperacion(clave);
    m.setObservacion(
        (r.idHabitacion() == null ? "Retirar" : "Asignar a " + r.idHabitacion())
            + " ("
            + r.estadoActualItem()
            + "): "
            + r.observacion().trim()
            + " ["
            + reglas.firma(r)
            + "]");
    movimientos.saveAndFlush(m);
    return respuesta(i);
  }

  @Override
  @Transactional
  public ItemResponseDTO conservacion(Long id, ItemConservacionDTO r) {
    reglas.validar(r);
    var u = reglas.usuario(r.idUsuario());
    var i = items.bloquear(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("El ítem no existe"));
    entityManager.refresh(i);
    String clave = hash("estado-item:" + r.claveOperacion());
    var previo = movimientos.findByClaveOperacion(clave);
    if (previo.isPresent()) {
      var m = previo.get();
      exigir(m.getItem() != null && m.getItem().getIdItem().equals(id)
              && m.getObservacion().endsWith("[" + reglas.firma(r) + "]"),
          "La clave de operación corresponde a otro cambio de conservación");
      return respuesta(i);
    }
    var actual = asignacionActual(i);
    exigir(i.getEstadoItem() == EstadoItem.EN_HABITACION && actual != null,
        "Asigna el ítem a una habitación antes de registrar su conservación");
    bloquearSinLimpieza(actual.getHabitacion().getIdHabitacion());
    actual = asignacionActual(i);
    registrarConservacion(actual, r.estadoActualItem(), u,
        r.observacion().trim() + " [" + reglas.firma(r) + "]", clave);
    return respuesta(i);
  }

  private void bloquearSinLimpieza(Long idHabitacion) {
    habitaciones.bloquear(idHabitacion)
        .orElseThrow(() -> new RecursoNoEncontradoException("La habitación no existe"));
    exigir(!informes.existsByHabitacionIdHabitacionAndEstadoInformeNot(idHabitacion, EstadoInforme.APROBADO),
        "El mobiliario de la habitación está en revisión de limpieza");
  }

  void registrarConservacion(DetalleHabitacion actual, EstadoConservacion estado, Usuario usuario,
      String observacion, String clave) {
    var d = new DetalleHabitacion();
    d.setItem(actual.getItem());
    d.setHabitacion(actual.getHabitacion());
    d.setEstadoActualItem(estado);
    detalles.saveAndFlush(d);
    var m = new MovimientoInventario();
    m.setItem(actual.getItem());
    m.setDetalleHabitacion(d);
    m.setUsuario(usuario);
    m.setTipoMovimientoAlmacen(reglas.tipo("EST_ITEM", 0));
    m.setCantidad(1);
    m.setStockResultante(0);
    m.setClaveOperacion(clave);
    m.setObservacion(observacion);
    movimientos.saveAndFlush(m);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ItemHistorialDTO> historial(Long id) {
    if (!items.existsById(id)) throw new RecursoNoEncontradoException("El ítem no existe");
    var lista = movimientos.findByItemIdItemOrderByIdMovimientoDesc(id);
    var resultado = new ArrayList<ItemHistorialDTO>();
    for (int n = 0; n < lista.size(); n++) {
      var m = lista.get(n);
      var origen = n + 1 < lista.size() ? destino(lista.get(n + 1)) : null;
      var destino = destino(m);
      String codigo = m.getTipoMovimientoAlmacen().getCodigo();
      String accion = "RET_ITEM".equals(codigo) ? "RETIRO"
          : "EST_ITEM".equals(codigo) ? "CONSERVACION"
          : origen != null && destino != null
              && !origen.getIdHabitacion().equals(destino.getIdHabitacion()) ? "TRASLADO" : "ASIGNACION";
      resultado.add(new ItemHistorialDTO(m.getIdMovimiento(), accion, m.getUsuario().getIdUsuario(),
          m.getUsuario().getUsername(), m.getFechaMovimiento(),
          origen == null ? null : origen.getIdHabitacion(), origen == null ? null : origen.getNumeroCuarto(),
          destino == null ? null : destino.getIdHabitacion(), destino == null ? null : destino.getNumeroCuarto(),
          m.getDetalleHabitacion() == null ? null : m.getDetalleHabitacion().getEstadoActualItem(),
          observacionPublica(m)));
    }
    return resultado;
  }

  private static Habitacion destino(MovimientoInventario m) {
    return "RET_ITEM".equals(m.getTipoMovimientoAlmacen().getCodigo()) || m.getDetalleHabitacion() == null
        ? null : m.getDetalleHabitacion().getHabitacion();
  }

  private static String observacionPublica(MovimientoInventario m) {
    String observacion = m.getObservacion();
    return m.getItem() == null || observacion == null ? observacion
        : observacion.replaceFirst(" \\[([0-9a-f]{64})\\]$", "");
  }

  @Override
  @Transactional(readOnly = true)
  public List<MovimientoRespuestaDTO> movimientos(Long id) {
    if (!items.existsById(id)) throw new RecursoNoEncontradoException("El ítem no existe");
    return movimientos.findByItemIdItemOrderByIdMovimientoDesc(id).stream()
        .map(ItemServiceImpl::movimiento)
        .toList();
  }

  public static MovimientoRespuestaDTO movimiento(MovimientoInventario m) {
    return new MovimientoRespuestaDTO(
        m.getIdMovimiento(),
        m.getClaveOperacion(),
        m.getUsuario().getIdUsuario(),
        m.getTipoMovimientoAlmacen().getNombreMovimiento(),
        m.getTipoMovimientoAlmacen().getSigno(),
        m.getCantidad(),
        m.getStockResultante(),
        m.getFechaMovimiento(),
        observacionPublica(m));
  }

  private DetalleHabitacion asignacionActual(Item i) {
    return movimientos
        .findFirstByItemIdItemOrderByIdMovimientoDesc(i.getIdItem())
        .map(MovimientoInventario::getDetalleHabitacion)
        .orElse(null);
  }

  private ItemResponseDTO respuesta(Item i) {
    var ultimo = asignacionActual(i);
    var d = i.getEstadoItem() == EstadoItem.EN_HABITACION ? ultimo : null;
    return new ItemResponseDTO(
        i.getIdItem(),
        i.getNombreItem(),
        i.getTipoItem().getIdTipoItem(),
        i.getEstadoItem(),
        d == null ? null : d.getIdDetalleHabitacion(),
        d == null ? null : d.getHabitacion().getIdHabitacion(),
        ultimo == null ? null : ultimo.getEstadoActualItem(),
        i.getTipoItem().getNombreItem(),
        d == null ? null : d.getHabitacion().getNumeroCuarto());
  }
}
