package pe.com.rebolsysBackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.com.rebolsysBackend.dto.ConfiguracionCatalogosDTO;
import pe.com.rebolsysBackend.service.service.ConfiguracionService;

@RestController
@RequestMapping("/api/v1/configuraciones")
@RequiredArgsConstructor
@Tag(name = "Configuración")
public class ConfiguracionController {
  private final ConfiguracionService servicio;

  @GetMapping("/catalogos")
  @Operation(summary = "Obtener tipos, categorías y estados para los formularios de configuración")
  public ConfiguracionCatalogosDTO catalogos() {
    return servicio.catalogos();
  }
}
