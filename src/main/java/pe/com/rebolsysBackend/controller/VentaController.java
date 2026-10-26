package pe.com.rebolsysBackend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.rebolsysBackend.dto.PaginaResponseDTO;
import pe.com.rebolsysBackend.dto.ReservaVentaResponseDTO;
import pe.com.rebolsysBackend.dto.VentaRequestDTO;
import pe.com.rebolsysBackend.dto.VentaResponseDTO;
import pe.com.rebolsysBackend.dto.VentaActualizarRequestDTO;
import pe.com.rebolsysBackend.service.service.VentaService;

@RestController
@RequestMapping("/api/v1/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @PostMapping
    public ResponseEntity<VentaResponseDTO> registrar(@Valid @RequestBody VentaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.registrar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VentaResponseDTO> actualizar(@PathVariable Long id, @Valid @RequestBody VentaActualizarRequestDTO request) {
        return ResponseEntity.ok(ventaService.actualizar(id, request));
    }

    @GetMapping
    public ResponseEntity<PaginaResponseDTO<VentaResponseDTO>> listar(
        @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int pagina,
        @RequestParam(defaultValue = "10") @Min(value = 1, message = "El tamaño debe ser positivo") @Max(
            value = 100,
            message = "El tamaño no debe superar 100"
        ) int tamanio
    ) {
        return ResponseEntity.ok(ventaService.listar(pagina, tamanio));
    }

    @GetMapping("/reservas")
    public ResponseEntity<List<ReservaVentaResponseDTO>> reservas(@RequestParam(defaultValue = "") String buscar) {
        return ResponseEntity.ok(ventaService.buscarReservas(buscar));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.buscar(id));
    }
}
