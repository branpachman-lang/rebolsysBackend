package pe.com.rebolsysBackend.controller;

import pe.com.rebolsysBackend.config.Acceso;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.com.rebolsysBackend.dto.LimpiezaDTO.Evidencia;
import pe.com.rebolsysBackend.service.impl.EvidenciaLimpiezaService;

@RestController
@RequestMapping(EvidenciaLimpiezaService.RUTA)
@RequiredArgsConstructor
@Tag(name = "Limpieza y mantenimiento")
public class EvidenciaLimpiezaController {
  private final EvidenciaLimpiezaService evidencias;

  @Acceso("LIMPIEZA:CARGAR_EVIDENCIA")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Cargar una foto de evidencia para un informe o ticket")
  public Evidencia guardar(@RequestPart("archivo") MultipartFile archivo) {
    return evidencias.guardar(archivo);
  }

  @Acceso("LIMPIEZA:VER_EVIDENCIA")
  @GetMapping(value = "/{nombre}", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE})
  @Operation(operationId = "descargarEvidenciaLimpieza", summary = "Descargar una foto de evidencia de limpieza")
  public ResponseEntity<Resource> obtener(@PathVariable String nombre) {
    var foto = evidencias.obtener(nombre);
    var tipo = nombre.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
    return ResponseEntity.ok().contentType(tipo).cacheControl(CacheControl.noStore()).body(foto);
  }
}
