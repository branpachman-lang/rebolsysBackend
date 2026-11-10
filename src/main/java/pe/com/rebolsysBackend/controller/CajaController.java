package pe.com.rebolsysBackend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.com.rebolsysBackend.config.Acceso;
import pe.com.rebolsysBackend.dto.CajaDTO.*;
import pe.com.rebolsysBackend.dto.PaginaResponseDTO;
import pe.com.rebolsysBackend.service.impl.CajaService;

@RestController
@RequestMapping("/api/v1/caja")
@Acceso({"CAJA:VER", "CAJA:CERRAR", "CAJA:REGISTRAR_INGRESO", "GASTOS:CREAR"})
@RequiredArgsConstructor
@Tag(name = "Caja")
public class CajaController {
  private final CajaService caja;

  @GetMapping("/actual")
  @Operation(summary = "Consultar el turno propio abierto o el último turno cerrado")
  public Contexto actual() { return caja.actual(); }

  @GetMapping("/turnos/{id}")
  @Operation(summary = "Consultar el arqueo esperado o guardado de un turno propio")
  public Contexto turno(@PathVariable @Positive Long id) { return caja.consultar(id); }

  @GetMapping("/turnos/{id}/movimientos")
  public PaginaResponseDTO<Movimiento> movimientos(@PathVariable @Positive Long id,
      @RequestParam(defaultValue = "0") @PositiveOrZero int pagina,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int tamanio) {
    return caja.movimientos(id, pagina, tamanio);
  }

  @PostMapping(value = "/turnos/{id}/movimientos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Acceso({"CAJA:REGISTRAR_INGRESO", "GASTOS:CREAR"})
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Registrar un ingreso manual o gasto en efectivo en el turno propio")
  public Movimiento registrar(@PathVariable @Positive Long id, @Valid @RequestPart("datos") RegistrarMovimiento datos,
      @RequestPart(value = "archivo", required = false) MultipartFile archivo, HttpServletRequest solicitud) {
    return caja.registrar(id, datos, archivo, solicitud.getRemoteAddr());
  }

  @PostMapping("/turnos/{id}/cierre")
  @Acceso("CAJA:CERRAR")
  @Operation(summary = "Guardar las declaraciones y cerrar el turno en una sola transacción")
  public Contexto cerrar(@PathVariable @Positive Long id, @Valid @RequestBody Cerrar datos, HttpServletRequest solicitud) {
    return caja.cerrar(id, datos, solicitud.getRemoteAddr());
  }

  @GetMapping("/movimientos/{id}/evidencia")
  public ResponseEntity<Resource> evidencia(@PathVariable @Positive Long id) {
    var descarga = caja.evidencia(id);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).cacheControl(CacheControl.noStore())
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
            .filename(descarga.nombre(), StandardCharsets.UTF_8).build().toString())
        .body(descarga.archivo());
  }
}
