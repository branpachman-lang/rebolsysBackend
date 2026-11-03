package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record HabitacionRequestDTO(
    @NotBlank @Size(max = 10) String numeroCuarto,
    @NotNull @Positive Long idTipoHabitacion,
    @NotNull EstadoHabitacion estadoHabitacion,
    @NotNull @Min(1) @Max(99) Integer capacidadPersonas,
    @NotNull @Min(1) @Max(99) Integer plazasCama,
    @NotNull @Min(0) @Max(98) Integer maxMenoresSinCama) {}
