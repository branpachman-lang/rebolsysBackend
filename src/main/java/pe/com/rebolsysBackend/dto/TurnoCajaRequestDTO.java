package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TurnoCajaRequestDTO {

    @NotNull(message = "idUsuario es obligatorio")
    private Long idUsuario;

    private LocalDateTime fechaCierre;

    @NotNull(message = "montoBase es obligatorio")
    @Digits(integer = 8, fraction = 2, message = "montoBase admite hasta 8 dígitos enteros y 2 decimales")
    @PositiveOrZero(message = "montoBase debe ser mayor o igual a 0")
    private BigDecimal montoBase = new BigDecimal("0.00");

    @NotNull(message = "estadoSesion es obligatorio")
    @Digits(integer = 1, fraction = 0, message = "estadoSesion admite hasta 1 dígitos enteros y 0 decimales")
    private Integer estadoSesion = 0;
}
