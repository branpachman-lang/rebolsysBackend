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
@io.swagger.v3.oas.annotations.tags.Tag(name = "Configuración de mobiliario")
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {
  private final ItemService servicio;

  @GetMapping
  public List<ItemResponseDTO> listar() {
    return servicio.readAll();
  }

  @GetMapping("/configuracion")
  @io.swagger.v3.oas.annotations.Operation(summary = "Buscar mobiliario por tipo, ubicación y conservación")
  public PaginaResponseDTO<ItemResponseDTO> configuracion(
      @Valid @org.springdoc.core.annotations.ParameterObject @ModelAttribute ItemFiltroDTO filtro) {
    return servicio.configuracion(filtro);
  }

  @PutMapping("/{id}/conservacion")
  @io.swagger.v3.oas.annotations.Operation(summary = "Registrar la conservación del mobiliario asignado")
  public ItemResponseDTO conservacion(@PathVariable Long id, @Valid @RequestBody ItemConservacionDTO r) {
    return servicio.conservacion(id, r);
  }

  @GetMapping("/{id}/historial")
  @io.swagger.v3.oas.annotations.Operation(summary = "Consultar asignaciones, traslados, retiros y conservación")
  public List<ItemHistorialDTO> historial(@PathVariable Long id) {
    return servicio.historial(id);
  }

  @GetMapping("/{id}")
  public ItemResponseDTO obtener(@PathVariable Long id) {
    return servicio.read(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ItemResponseDTO crear(@Valid @RequestBody ItemRequestDTO r) {
    return servicio.create(r);
  }

  @PutMapping("/{id}")
  public ItemResponseDTO editar(@PathVariable Long id, @Valid @RequestBody ItemRequestDTO r) {
    return servicio.update(id, r);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long id) {
    servicio.delete(id);
  }

  @PutMapping("/{id}/asignacion")
  public ItemResponseDTO asignar(@PathVariable Long id, @Valid @RequestBody ItemAsignacionDTO r) {
    return servicio.asignar(id, r);
  }

  @GetMapping("/{id}/movimientos")
  public List<MovimientoRespuestaDTO> movimientos(@PathVariable Long id) {
    return servicio.movimientos(id);
  }
}
