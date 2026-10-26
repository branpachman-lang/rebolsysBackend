package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoHabitacionRequestDTO {

    @NotNull(message = "nombreTipo es obligatorio")
    @Size(min = 1, max = 30, message = "nombreTipo debe tener entre 1 y 30 caracteres")
    private String nombreTipo;

    @NotNull(message = "capacidadMax es obligatorio")
    @Digits(integer = 2, fraction = 0, message = "capacidadMax admite hasta 2 dígitos enteros y 0 decimales")
    @Positive(message = "capacidadMax debe ser mayor que 0")
    private Integer capacidadMax;

    @NotNull(message = "precioBase es obligatorio")
    @Digits(integer = 8, fraction = 2, message = "precioBase admite hasta 8 dígitos enteros y 2 decimales")
    @PositiveOrZero(message = "precioBase debe ser mayor o igual a 0")
    private BigDecimal precioBase = new BigDecimal("0");
}
