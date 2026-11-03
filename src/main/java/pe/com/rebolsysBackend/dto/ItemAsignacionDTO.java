package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record ItemAsignacionDTO(
    @NotBlank @Size(max = 64) String claveOperacion,
    @NotNull @Positive Long idUsuario,
    @Positive Long idHabitacion,
    @NotNull EstadoConservacion estadoActualItem,
    @NotBlank @Size(max = 180) String observacion) {}
