package pe.com.rebolsysBackend.controller;

import pe.com.rebolsysBackend.config.Acceso;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
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
  private final ReservaService reservaservice;

  @Acceso("RESERVAS:CREAR")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Registrar reserva asistida con ocupantes opcionales")
  public ReservaRespuestaDTO registrar(@Valid @RequestBody ReservaRegistroRequestDTO r) {
    return reservaservice.registrar(r);
  }

  @Acceso("RESERVAS:VER")
  @GetMapping("/{id}")
  public ReservaRespuestaDTO obtener(@PathVariable Long id) {
    return reservaservice.obtener(id);
  }

  @Acceso("RESERVAS:VER")
  @GetMapping
  public PaginaResponseDTO<ReservaRespuestaDTO> listar(
      @RequestParam(defaultValue = "0") @Min(0) int pagina,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int tamanio) {
    return reservaservice.listar(pagina, tamanio);
  }

  @Acceso("RESERVAS:REGISTRAR_INGRESO")
  @PostMapping("/detalles/{id}/ingreso")
  public ReservaDetalleRespuestaDTO ingreso(@PathVariable Long id, @Valid @RequestBody OperacionUsuarioDTO r) {
    return reservaservice.cambiarEstado(id, "ingreso", r);
  }

  @Acceso("RESERVAS:REGISTRAR_SALIDA")
  @PostMapping("/detalles/{id}/salida")
  public ReservaDetalleRespuestaDTO salida(@PathVariable Long id, @Valid @RequestBody OperacionUsuarioDTO r) {
    return reservaservice.cambiarEstado(id, "salida", r);
  }

  @Acceso("RESERVAS:CANCELAR")
  @PostMapping("/detalles/{id}/cancelacion")
  @Operation(operationId = "cancelarEstancia")
  public ReservaDetalleRespuestaDTO cancelar(@PathVariable Long id, @Valid @RequestBody OperacionUsuarioDTO r) {
    return reservaservice.cambiarEstado(id, "cancelacion", r);
  }

  @Acceso("RESERVAS:EDITAR_OCUPANTES")
  @PutMapping("/detalles/{id}/ocupantes")
  @Operation(summary = "Completar o corregir ocupantes antes del check-in")
  public ReservaDetalleRespuestaDTO ocupantes(
      @PathVariable Long id, @Valid @RequestBody ReservaOcupantesGuardarDTO r) {
    return reservaservice.guardarOcupantes(id, r);
  }
}
