package pe.com.rebolsysBackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.rebolsysBackend.dto.CatalogosResponseDTO;
import pe.com.rebolsysBackend.service.service.CatalogoService;

@RestController
@RequestMapping("/api/v1/recepcion/catalogos")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping
    public ResponseEntity<CatalogosResponseDTO> listar() {
        return ResponseEntity.ok(catalogoService.catalogos());
    }
}
