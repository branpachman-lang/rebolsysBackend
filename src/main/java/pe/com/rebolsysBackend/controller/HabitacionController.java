package pe.com.rebolsysBackend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.service.service.*;

@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "Configuración de habitaciones")
@RequestMapping("/api/v1/habitaciones")
@RequiredArgsConstructor
public class HabitacionController {
  private final HabitacionService servicio;

  @GetMapping
  public List<HabitacionEstadoResponseDTO> estado(
      @RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
    return servicio.estado(desde, hasta);
  }

  @GetMapping("/configuracion")
  public List<HabitacionResponseDTO> listar() {
    return servicio.readAll();
  }

  @GetMapping("/configuracion/pagina")
  @io.swagger.v3.oas.annotations.Operation(summary = "Buscar y filtrar la configuración de habitaciones")
  public PaginaResponseDTO<HabitacionResponseDTO> configuracion(
      @Valid @org.springdoc.core.annotations.ParameterObject @ModelAttribute HabitacionFiltroDTO filtro) {
    return servicio.configuracion(filtro);
  }

  @PatchMapping("/{id}/estado")
  @io.swagger.v3.oas.annotations.Operation(summary = "Cambiar el estado físico de una habitación")
  public HabitacionResponseDTO cambiarEstado(
      @PathVariable Long id, @Valid @RequestBody HabitacionEstadoGuardarDTO r) {
    return servicio.cambiarEstado(id, r);
  }

  @GetMapping("/{id}")
  public HabitacionResponseDTO obtener(@PathVariable Long id) {
    return servicio.read(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public HabitacionResponseDTO crear(@Valid @RequestBody HabitacionRequestDTO r) {
    return servicio.create(r);
  }

  @PutMapping("/{id}")
  public HabitacionResponseDTO editar(
      @PathVariable Long id, @Valid @RequestBody HabitacionRequestDTO r) {
    return servicio.update(id, r);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long id) {
    servicio.delete(id);
  }

  @GetMapping("/{id}/items")
  public List<ItemResponseDTO> mobiliario(@PathVariable Long id) {
    return servicio.mobiliario(id);
  }
}
