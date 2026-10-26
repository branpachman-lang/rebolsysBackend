package pe.com.rebolsysBackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.rebolsysBackend.dto.HealthResponseDTO;

@RestController
@RequestMapping("/api")
@Tag(name = "Estado del servidor")
public class HealthController {

    @GetMapping("/health")
    @Operation(
        summary = "Consultar el arranque del servidor",
        description = "Confirma que la API responde; no verifica la conexión con Oracle."
    )
    public HealthResponseDTO health() {
        return new HealthResponseDTO("rebolsysBackend", "UP");
    }
}
