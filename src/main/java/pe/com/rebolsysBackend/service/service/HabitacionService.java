package pe.com.rebolsysBackend.service.service;

import java.time.LocalDate;
import java.util.List;
import pe.com.rebolsysBackend.dto.HabitacionEstadoResponseDTO;
import pe.com.rebolsysBackend.dto.HabitacionOcupacionRequestDTO;
import pe.com.rebolsysBackend.dto.HabitacionOcupacionResponseDTO;

public interface HabitacionService {
    List<HabitacionEstadoResponseDTO> estado(LocalDate desde, LocalDate hasta);
    HabitacionOcupacionResponseDTO actualizarOcupacion(Long id, HabitacionOcupacionRequestDTO request);
}
