package pe.com.rebolsysBackend.controller;

import pe.com.rebolsysBackend.config.Acceso;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.service.service.*;
import pe.com.rebolsysBackend.service.impl.SesionOperacionService;

@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "Configuración de habitaciones")
@RequestMapping("/api/v1/habitaciones")
@RequiredArgsConstructor
public class HabitacionController {
  private final HabitacionService servicio;

  @Acceso({"HABITACIONES:VER", "RESERVAS:CREAR"})
  @GetMapping
  public List<HabitacionEstadoResponseDTO> estado(
      @RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
    var habitaciones = servicio.estado(desde, hasta);
    var actual = SesionOperacionService.enPeticion();
    if (actual != null && !actual.tiene("RESERVAS:VER")) {
      habitaciones.forEach(h -> h.reservas().forEach(r -> r.setCliente("Ocupación reservada")));
    }
    return habitaciones;
  }

  @Acceso({"HABITACIONES:VER", "MOBILIARIO:ASIGNAR"})
  @GetMapping("/configuracion")
  public List<HabitacionResponseDTO> listar() {
    return servicio.readAll();
  }

  @Acceso("HABITACIONES:VER")
  @GetMapping("/configuracion/pagina")
  @io.swagger.v3.oas.annotations.Operation(summary = "Buscar y filtrar la configuración de habitaciones")
  public PaginaResponseDTO<HabitacionResponseDTO> configuracion(
      @Valid @org.springdoc.core.annotations.ParameterObject @ModelAttribute HabitacionFiltroDTO filtro) {
    return servicio.configuracion(filtro);
  }

  @Acceso("HABITACIONES:CAMBIAR_ESTADO")
  @PatchMapping("/{id}/estado")
  @io.swagger.v3.oas.annotations.Operation(summary = "Cambiar el estado físico de una habitación")
  public HabitacionResponseDTO cambiarEstado(
      @PathVariable Long id, @Valid @RequestBody HabitacionEstadoGuardarDTO r) {
    return servicio.cambiarEstado(id, r);
  }

  @Acceso("HABITACIONES:VER")
  @GetMapping("/{id}")
  public HabitacionResponseDTO obtener(@PathVariable Long id) {
    return servicio.read(id);
  }

  @Acceso("HABITACIONES:CREAR")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public HabitacionResponseDTO crear(@Valid @RequestBody HabitacionRequestDTO r) {
    return servicio.create(r);
  }

  @Acceso("HABITACIONES:EDITAR")
  @PutMapping("/{id}")
  public HabitacionResponseDTO editar(
      @PathVariable Long id, @Valid @RequestBody HabitacionRequestDTO r) {
    return servicio.update(id, r);
  }

  @Acceso("HABITACIONES:ELIMINAR")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long id) {
    servicio.delete(id);
  }

  @Acceso({"HABITACIONES:VER", "MOBILIARIO:VER"})
  @GetMapping("/{id}/items")
  public List<ItemResponseDTO> mobiliario(@PathVariable Long id) {
    return servicio.mobiliario(id);
  }
}
