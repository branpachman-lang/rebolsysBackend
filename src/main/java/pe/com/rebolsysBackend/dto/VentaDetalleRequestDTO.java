package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VentaDetalleRequestDTO {

    @NotNull(message = "Selecciona el producto")
    private Long idProducto;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor que cero")
    @Max(value = 999999999, message = "La cantidad excede el límite de unidades")
    private Integer cantidad;

    @DecimalMin(value = "0", message = "El precio esperado no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio esperado admite 8 dígitos enteros y 2 decimales")
    private java.math.BigDecimal precioEsperado;

    public VentaDetalleRequestDTO(Long idProducto, Integer cantidad) {
        this(idProducto, cantidad, null);
    }
}
