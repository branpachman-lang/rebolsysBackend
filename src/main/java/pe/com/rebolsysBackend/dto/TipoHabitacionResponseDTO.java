package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;

public record TipoHabitacionResponseDTO(
    Long idTipoHabitacion, String nombreTipo, Integer capacidadMax, BigDecimal precioBase) {}
