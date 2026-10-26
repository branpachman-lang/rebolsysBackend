package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class PagoRequestDTO {

    @NotNull(message = "idComprobante es obligatorio")
    private Long idComprobante;

    @NotNull(message = "idFormaPago es obligatorio")
    private Long idFormaPago;

    @NotNull(message = "idUsuario es obligatorio")
    private Long idUsuario;

    @NotNull(message = "idTurno es obligatorio")
    private Long idTurno;

    @NotNull(message = "monto es obligatorio")
    @Digits(integer = 8, fraction = 2, message = "monto admite hasta 8 dígitos enteros y 2 decimales")
    @Positive(message = "monto debe ser mayor que 0")
    private BigDecimal monto;

    @Size(max = 50, message = "numeroOperacion no debe superar 50 caracteres")
    private String numeroOperacion;

    @NotNull(message = "verificado es obligatorio")
    @Digits(integer = 1, fraction = 0, message = "verificado admite hasta 1 dígitos enteros y 0 decimales")
    @Min(value = 0, message = "verificado debe ser mayor o igual a 0")
    @Max(value = 1, message = "verificado no debe superar 1")
    private Integer verificado = 0;

    @Size(max = 64, message = "claveIdempotencia no debe superar 64 caracteres")
    private String claveIdempotencia;
}
