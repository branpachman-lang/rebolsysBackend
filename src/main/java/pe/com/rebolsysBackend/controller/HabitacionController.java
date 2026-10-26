package pe.com.rebolsysBackend.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.rebolsysBackend.dto.HabitacionEstadoResponseDTO;
import pe.com.rebolsysBackend.dto.HabitacionOcupacionRequestDTO;
import pe.com.rebolsysBackend.dto.HabitacionOcupacionResponseDTO;
import pe.com.rebolsysBackend.service.service.HabitacionService;

@RestController
@RequestMapping("/api/v1/habitaciones")
public class HabitacionController {

    private final HabitacionService habitacionService;

    public HabitacionController(HabitacionService habitacionService) {
        this.habitacionService = habitacionService;
    }

    @GetMapping
    public ResponseEntity<List<HabitacionEstadoResponseDTO>> listar(
        @RequestParam LocalDate desde,
        @RequestParam LocalDate hasta
    ) {
        return ResponseEntity.ok(habitacionService.estado(desde, hasta));
    }

    @PutMapping("/{id}/ocupacion")
    public ResponseEntity<HabitacionOcupacionResponseDTO> actualizarOcupacion(
        @PathVariable Long id,
        @Valid @RequestBody HabitacionOcupacionRequestDTO request
    ) {
        return ResponseEntity.ok(habitacionService.actualizarOcupacion(id, request));
    }
}
