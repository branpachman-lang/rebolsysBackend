package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record HabitacionResponseDTO(
    Long idHabitacion,
    String numeroCuarto,
    Long idTipoHabitacion,
    String nombreTipo,
    EstadoHabitacion estadoHabitacion,
    int capacidadPersonas,
    int plazasCama,
    int maxMenoresSinCama,
    java.math.BigDecimal precioBase) {}
