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
public class HabitacionServiceImpl implements HabitacionService {
  private final jakarta.persistence.EntityManager entityManager;
  private final HabitacionRepository habitaciones;
  private final TipoHabitacionRepository tipos;
  private final DetalleReservaRepository detalles;
  private final ReservaOcupanteRepository ocupantes;
  private final ClienteService clientes;
  private final ReglasPropuesta reglas;
  private final DetalleHabitacionRepository mobiliario;
  private final ItemService items;

  @Override
  @Transactional
  public HabitacionResponseDTO create(HabitacionRequestDTO r) {
    return guardar(null, r);
  }

  @Override
  @Transactional
  public HabitacionResponseDTO update(Long id, HabitacionRequestDTO r) {
    Objects.requireNonNull(id, "El identificador es obligatorio");
    return guardar(id, r);
  }

  @Override
  @Transactional(readOnly = true)
  public HabitacionResponseDTO read(Long id) {
    return obtener(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<HabitacionResponseDTO> readAll() {
    return listar();
  }

  @Override
  @Transactional
  public void delete(Long id) {
    var h = habitaciones.bloquear(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("La habitación no existe"));
    exigir(detalles.findByHabitacionIdHabitacion(id).isEmpty(),
        "No se puede eliminar una habitación con historial de reservas");
    exigir(mobiliario.findByHabitacionIdHabitacion(id).isEmpty(),
        "No se puede eliminar una habitación con historial de mobiliario");
    habitaciones.delete(h);
    habitaciones.flush();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ItemResponseDTO> mobiliario(Long id) {
    if (!habitaciones.existsById(id))
      throw new RecursoNoEncontradoException("La habitación no existe");
    return items.enHabitacion(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<HabitacionEstadoResponseDTO> estado(LocalDate desde, LocalDate hasta) {
    exigir(
        desde != null && hasta != null && hasta.isAfter(desde),
        "La fecha final debe ser posterior a la inicial");
    var periodo = detalles.buscarEnPeriodo(desde.atStartOfDay(), hasta.atStartOfDay());
    return habitaciones.findAll(Sort.by("numeroCuarto")).stream()
        .map(
            h -> {
              var bloques =
                  periodo.stream()
                      .filter(d -> d.getHabitacion().getIdHabitacion().equals(h.getIdHabitacion()))
                      .toList();
              boolean enCurso =
                  detalles.findByHabitacionIdHabitacion(h.getIdHabitacion()).stream()
                      .anyMatch(d -> d.getEstadoDetalle() == EstadoReserva.EN_CURSO);
              var reservas =
                  bloques.stream()
                      .map(
                          d ->
                              new HabitacionReservaResponseDTO(
                                  "R" + d.getReserva().getIdReserva(),
                                  clientes.nombre(d.getReserva().getCliente().getPerfil()),
                                  d.getFechaIngreso(),
                                  d.getFechaSalida(),
                                  d.getEstadoDetalle().name()))
                      .toList();
              return new HabitacionEstadoResponseDTO(
                  h.getIdHabitacion(),
                  h.getNumeroCuarto(),
                  h.getTipoHabitacion().getNombreTipo(),
                  h.getCapacidadPersonas(),
                  h.getPlazasCama(),
                  h.getMaxMenoresSinCama(),
                  h.getTipoHabitacion().getPrecioBase(),
                  h.getEstadoHabitacion(),
                  enCurso,
                  h.getEstadoHabitacion() == EstadoHabitacion.DISPONIBLE
                      && bloques.isEmpty()
                      && !enCurso,
                  reservas);
            })
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<HabitacionResponseDTO> listar() {
    return habitaciones.findAll(Sort.by("numeroCuarto")).stream().map(this::respuesta).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public PaginaResponseDTO<HabitacionResponseDTO> configuracion(HabitacionFiltroDTO f) {
    reglas.validar(f);
    if (f.idTipoHabitacion() != null && !tipos.existsById(f.idTipoHabitacion()))
      throw new RecursoNoEncontradoException("El tipo de habitación no existe");
    var page = habitaciones.buscarConfiguracion(patronBusqueda(f.buscar()), f.idTipoHabitacion(),
        f.estado(), f.capacidadMinima(), PageRequest.of(f.pagina(), f.tamanio(),
            Sort.by("numeroCuarto", "idHabitacion")));
    return new PaginaResponseDTO<>(page.map(this::respuesta).getContent(), f.pagina(), f.tamanio(),
        page.getTotalElements(), page.getTotalPages(), page.isLast());
  }

  @Override
  @Transactional
  public HabitacionResponseDTO cambiarEstado(Long id, HabitacionEstadoGuardarDTO r) {
    reglas.validar(r);
    var h = habitaciones.bloquear(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("La habitación no existe"));
    entityManager.refresh(h);
    boolean enCurso = detalles.findByHabitacionIdHabitacion(id).stream()
        .anyMatch(d -> d.getEstadoDetalle() == EstadoReserva.EN_CURSO);
    exigir((r.estadoHabitacion() == EstadoHabitacion.OCUPADO) == enCurso,
        "El estado OCUPADO solo corresponde a una estancia en curso");
    h.setEstadoHabitacion(r.estadoHabitacion());
    return respuesta(habitaciones.saveAndFlush(h));
  }

  @Override
  @Transactional(readOnly = true)
  public HabitacionResponseDTO obtener(Long id) {
    return respuesta(
        habitaciones
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("La habitación no existe")));
  }

  @Override
  @Transactional
  public HabitacionResponseDTO guardar(Long id, HabitacionRequestDTO r) {
    reglas.validar(r);
    // Mismo orden que al editar el tipo: tipo antes de habitación.
    var tipo = tipos.bloquear(r.idTipoHabitacion())
        .orElseThrow(() -> new RecursoNoEncontradoException("El tipo de habitación no existe"));
    entityManager.refresh(tipo);
    var h =
        id == null
            ? new Habitacion()
            : habitaciones
                .bloquear(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La habitación no existe"));
    if (id != null) entityManager.refresh(h);
    exigir(id == null ? !habitaciones.existsByNumeroCuarto(r.numeroCuarto().trim())
            : !habitaciones.existsByNumeroCuartoAndIdHabitacionNot(r.numeroCuarto().trim(), id),
        "Ya existe una habitación con ese número");
    exigir(
        r.capacidadPersonas() <= tipo.getCapacidadMax(),
        "La capacidad supera el máximo del tipo de habitación");
    exigir(
        r.plazasCama() <= r.capacidadPersonas() && r.maxMenoresSinCama() < r.capacidadPersonas(),
        "Las plazas y el límite de menores deben respetar la capacidad");
    var estancias =
        id == null ? List.<DetalleReserva>of() : detalles.findByHabitacionIdHabitacion(id);
    boolean enCurso =
        estancias.stream().anyMatch(d -> d.getEstadoDetalle() == EstadoReserva.EN_CURSO);
    exigir(
        (r.estadoHabitacion() == EstadoHabitacion.OCUPADO) == enCurso,
        "El estado OCUPADO solo corresponde a una estancia en curso");
    if (id != null) {
      Map<Long, List<ReservaOcupante>> porDetalle = new HashMap<>();
      for (var o : ocupantes.activosHabitacion(id))
        porDetalle
            .computeIfAbsent(o.getDetalleReserva().getIdDetalleReserva(), k -> new ArrayList<>())
            .add(o);
      for (var grupo : porDetalle.values()) {
        long camas = grupo.stream().filter(o -> o.getOcupaPlazaCama() == 1).count();
        exigir(
            grupo.size() <= r.capacidadPersonas()
                && camas <= r.plazasCama()
                && grupo.size() - camas <= r.maxMenoresSinCama(),
            "La nueva capacidad no admite a los ocupantes de una reserva activa");
      }
    }
    h.setNumeroCuarto(r.numeroCuarto().trim());
    h.setTipoHabitacion(tipo);
    h.setEstadoHabitacion(r.estadoHabitacion());
    h.setCapacidadPersonas(r.capacidadPersonas());
    h.setPlazasCama(r.plazasCama());
    h.setMaxMenoresSinCama(r.maxMenoresSinCama());
    return respuesta(habitaciones.saveAndFlush(h));
  }

  private HabitacionResponseDTO respuesta(Habitacion h) {
    return new HabitacionResponseDTO(
        h.getIdHabitacion(),
        h.getNumeroCuarto(),
        h.getTipoHabitacion().getIdTipoHabitacion(),
        h.getTipoHabitacion().getNombreTipo(),
        h.getEstadoHabitacion(),
        h.getCapacidadPersonas(),
        h.getPlazasCama(),
        h.getMaxMenoresSinCama(),
        h.getTipoHabitacion().getPrecioBase());
  }
}
