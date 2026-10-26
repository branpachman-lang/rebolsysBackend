package pe.com.rebolsysBackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
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
public class ComprobanteDetalleRequestDTO {

    @NotNull(message = "idComprobante es obligatorio")
    private Long idComprobante;

    @NotNull(message = "idConsumo es obligatorio")
    private Long idConsumo;

    @NotNull(message = "numeroLinea es obligatorio")
    @Digits(integer = 4, fraction = 0, message = "numeroLinea admite hasta 4 dígitos enteros y 0 decimales")
    @Positive(message = "numeroLinea debe ser mayor que 0")
    private Integer numeroLinea;

    @NotNull(message = "descripcion es obligatorio")
    @Size(min = 1, max = 300, message = "descripcion debe tener entre 1 y 300 caracteres")
    private String descripcion;

    @NotNull(message = "cantidad es obligatorio")
    @Digits(integer = 9, fraction = 3, message = "cantidad admite hasta 9 dígitos enteros y 3 decimales")
    @Positive(message = "cantidad debe ser mayor que 0")
    private BigDecimal cantidad;

    @NotNull(message = "valorUnitario es obligatorio")
    @Digits(integer = 8, fraction = 4, message = "valorUnitario admite hasta 8 dígitos enteros y 4 decimales")
    @PositiveOrZero(message = "valorUnitario debe ser mayor o igual a 0")
    private BigDecimal valorUnitario;

    @NotNull(message = "subtotal es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "subtotal admite hasta 10 dígitos enteros y 2 decimales")
    @PositiveOrZero(message = "subtotal debe ser mayor o igual a 0")
    private BigDecimal subtotal;

    @NotNull(message = "igv es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "igv admite hasta 10 dígitos enteros y 2 decimales")
    @PositiveOrZero(message = "igv debe ser mayor o igual a 0")
    private BigDecimal igv;

    @NotNull(message = "total es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "total admite hasta 10 dígitos enteros y 2 decimales")
    private BigDecimal total;

    private Long idHabitacion;

    @JsonIgnore
    @AssertTrue(message = "El total debe ser igual a subtotal más IGV")
    public boolean isTotalValido() {
        return subtotal == null || igv == null || total == null || total.compareTo(subtotal.add(igv)) == 0;
    }
}
