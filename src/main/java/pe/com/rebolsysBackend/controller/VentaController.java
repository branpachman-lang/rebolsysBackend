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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/ventas")
@RequiredArgsConstructor
@Tag(name = "Ventas de productos")
public class VentaController {
  private final VentaService servicio;

  @Acceso("VENTAS:CREAR")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Crear borrador de venta con varios productos")
  public VentaResponseDTO registrar(@Valid @RequestBody VentaRequestDTO r) {
    return servicio.registrar(r);
  }

  //venta producto
  @Acceso("VENTAS:CREAR_Y_CONFIRMAR")
  @PostMapping("/confirmadas")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Registrar y confirmar venta completa en una sola operación")
  public VentaResponseDTO registrarConfirmada(@Valid @RequestBody VentaRequestDTO r) {
    return servicio.registrarConfirmada(r);
  }

  @Acceso("VENTAS:EDITAR")
  @PutMapping("/{id}")
  @Operation(summary = "Editar comprador y productos de una venta en borrador")
  public VentaResponseDTO editar(@PathVariable Long id, @Valid @RequestBody VentaEditarDTO r) {
    return servicio.editar(id, r);
  }

  @Acceso("VENTAS:VER")
  @GetMapping("/{id}")
  public VentaResponseDTO obtener(@PathVariable Long id) {
    return servicio.obtener(id);
  }

  @Acceso("VENTAS:VER")
  @GetMapping
  public PaginaResponseDTO<VentaResponseDTO> listar(
      @RequestParam(defaultValue = "0") @Min(0) int pagina,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int tamanio) {
    return servicio.listar(pagina, tamanio);
  }

  @Acceso("VENTAS:CONFIRMAR")
  @PostMapping("/{id}/confirmacion")
  @Operation(summary = "Confirmar el carrito y descontar stock de todos sus productos")
  public VentaResponseDTO confirmar(
      @PathVariable Long id, @Valid @RequestBody OperacionUsuarioDTO r) {
    return servicio.confirmar(id, r);
  }

  @Acceso("VENTAS:ANULAR")
  @PostMapping("/{id}/anulacion")
  @Operation(summary = "Anular venta sin cuenta ni cobros y reponer stock")
  public VentaResponseDTO anular(@PathVariable Long id, @Valid @RequestBody OperacionUsuarioDTO r) {
    return servicio.anular(id, r);
  }
}
