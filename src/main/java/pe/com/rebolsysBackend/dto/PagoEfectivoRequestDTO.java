package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PagoEfectivoRequestDTO {

    @NotBlank(message = "clavePago es obligatorio")
    @Size(max = 64, message = "clavePago no debe superar 64 caracteres")
    private String clavePago;

    @NotNull(message = "efectivoRecibido es obligatorio")
    @DecimalMin(value = "0", message = "efectivoRecibido debe ser mayor o igual a 0")
    @Digits(integer = 8, fraction = 2, message = "efectivoRecibido admite hasta 8 dígitos enteros y 2 decimales")
    private BigDecimal efectivoRecibido;

    @DecimalMin(value = "0.01", message = "El importe del pago debe ser mayor que cero")
    @Digits(integer = 8, fraction = 2, message = "El importe del pago admite hasta 8 dígitos enteros y 2 decimales")
    private BigDecimal montoPago;

    public PagoEfectivoRequestDTO(String clavePago, BigDecimal efectivoRecibido) {
        this(clavePago, efectivoRecibido, null);
    }
}
