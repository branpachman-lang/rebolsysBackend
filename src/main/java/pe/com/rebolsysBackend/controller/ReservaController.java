package pe.com.rebolsysBackend.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.service.service.*;

@RestController
@RequestMapping("/api/v1/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas")
public class ReservaController {
  private final ReservaService servicio;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Registrar reserva asistida con ocupantes opcionales")
  public ReservaRespuestaDTO registrar(@Valid @RequestBody ReservaRegistroRequestDTO r) {
    return servicio.registrar(r);
  }

  @GetMapping("/{id}")
  public ReservaRespuestaDTO obtener(@PathVariable Long id) {
    return servicio.obtener(id);
  }

  @GetMapping
  public PaginaResponseDTO<ReservaRespuestaDTO> listar(
      @RequestParam(defaultValue = "0") @Min(0) int pagina,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int tamanio) {
    return servicio.listar(pagina, tamanio);
  }

  @PostMapping("/detalles/{id}/{accion:ingreso|salida|cancelacion}")
  public ReservaDetalleRespuestaDTO estado(
      @PathVariable Long id,
      @PathVariable String accion,
      @Valid @RequestBody OperacionUsuarioDTO r) {
    return servicio.cambiarEstado(id, accion, r);
  }

  @PutMapping("/detalles/{id}/ocupantes")
  @Operation(summary = "Completar o corregir ocupantes antes del check-in")
  public ReservaDetalleRespuestaDTO ocupantes(
      @PathVariable Long id, @Valid @RequestBody ReservaOcupantesGuardarDTO r) {
    return servicio.guardarOcupantes(id, r);
  }
}
