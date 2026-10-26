package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArqueoCajaRequestDTO {

    @NotNull(message = "idTurno es obligatorio")
    private Long idTurno;

    @NotNull(message = "idFormaPago es obligatorio")
    private Long idFormaPago;

    @NotNull(message = "montoCalculado es obligatorio")
    @Digits(integer = 8, fraction = 2, message = "montoCalculado admite hasta 8 dígitos enteros y 2 decimales")
    @PositiveOrZero(message = "montoCalculado debe ser mayor o igual a 0")
    private BigDecimal montoCalculado;

    @NotNull(message = "montoDeclarado es obligatorio")
    @Digits(integer = 8, fraction = 2, message = "montoDeclarado admite hasta 8 dígitos enteros y 2 decimales")
    @PositiveOrZero(message = "montoDeclarado debe ser mayor o igual a 0")
    private BigDecimal montoDeclarado;
}
