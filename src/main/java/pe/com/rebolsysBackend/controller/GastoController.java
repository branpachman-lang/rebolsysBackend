package pe.com.rebolsysBackend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.service.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

@RestController
@RequestMapping("/api/v1/gastos")
@RequiredArgsConstructor
@Tag(name = "Gastos")
public class GastoController {
  private final GastoService servicio;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Registrar gasto del turno abierto del recepcionista")
  public GastoResponseDTO registrar(@Valid @RequestBody GastoRequestDTO r) {
    return servicio.registrar(r);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Consultar detalle de un gasto y su responsable")
  public GastoResponseDTO obtener(@PathVariable Long id) {
    return servicio.obtener(id);
  }

  @GetMapping
  @Operation(summary = "Consultar listado anterior de gastos de un turno")
  public List<GastoResponseDTO> listar(@RequestParam @Positive Long idTurno) {
    return servicio.listar(idTurno);
  }

  @GetMapping("/historial")
  @Operation(summary = "Consultar historial filtrable de gastos y total gastado",
          description = "Fechas inclusivas en America/Lima. Página desde cero y tamaño por defecto 20. "
                  + "El resumen usa todos los resultados filtrados, sin limitarse a la página.")
  public GastoHistorialDTO historial(@Valid @ParameterObject @ModelAttribute GastoFiltroDTO filtros) {
    return servicio.historial(filtros);
  }

  @GetMapping("/tipos")
  @Operation(summary = "Consultar categorías disponibles para registrar o filtrar gastos")
  public List<CatalogoItemResponseDTO> tipos() {
    return servicio.tipos();
  }
}
