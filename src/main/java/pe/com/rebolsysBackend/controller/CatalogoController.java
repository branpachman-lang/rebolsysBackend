package pe.com.rebolsysBackend.controller;

import pe.com.rebolsysBackend.config.Acceso;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.rebolsysBackend.dto.CatalogosResponseDTO;
import pe.com.rebolsysBackend.service.service.CatalogoService;
import pe.com.rebolsysBackend.service.impl.SesionOperacionService;

@RestController
@RequestMapping("/api/v1/recepcion/catalogos")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @Acceso("@CATALOGOS")
    @GetMapping
    public ResponseEntity<CatalogosResponseDTO> listar() {
        var datos = catalogoService.catalogos();
        var actual = SesionOperacionService.enPeticion();
        if (actual == null || "PRUEBA".equals(actual.modo())) return ResponseEntity.ok(datos);
        return ResponseEntity.ok(new CatalogosResponseDTO(datos.tiposDocumento(), datos.formasPago(),
            datos.tiposHabitacion(), datos.tiposItem(), datos.tiposGasto(),
            datos.usuarios().stream().filter(u -> u.getId().equals(actual.idUsuario())).toList(),
            datos.turnosAbiertos().stream().filter(t -> t.idUsuario().equals(actual.idUsuario())).toList()));
    }
}
