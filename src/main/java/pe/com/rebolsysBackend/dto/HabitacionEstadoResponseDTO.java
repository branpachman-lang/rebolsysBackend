package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record HabitacionEstadoResponseDTO(
    Long idHabitacion,
    String numero,
    String tipo,
    int capacidad,
    int plazasCama,
    int maxMenoresSinCama,
    BigDecimal precio,
    EstadoHabitacion estado,
    boolean ocupadaActualmente,
    boolean disponible,
    List<HabitacionReservaResponseDTO> reservas) {}
