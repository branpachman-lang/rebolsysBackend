package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record TipoHabitacionRequestDTO(
    @NotBlank @Size(max = 30) String nombreTipo,
    @NotNull @Min(1) @Max(99) Integer capacidadMax,
    @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal precioBase) {}
