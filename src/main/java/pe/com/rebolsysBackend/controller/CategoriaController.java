package pe.com.rebolsysBackend.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.rebolsysBackend.dto.CategoriaRequestDTO;
import pe.com.rebolsysBackend.dto.CategoriaResponseDTO;
import pe.com.rebolsysBackend.service.service.CategoriaService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "Configuración de productos")
@RequestMapping("/api/v1/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public ResponseEntity<Iterable<CategoriaResponseDTO>> readAll() {
        return ResponseEntity.ok(categoriaService.readAll());
    }

    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> create(@Valid @RequestBody CategoriaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> read(@PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.read(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> update(
            @PathVariable Long id, @Valid @RequestBody CategoriaRequestDTO request) {
        return ResponseEntity.ok(categoriaService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoriaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
