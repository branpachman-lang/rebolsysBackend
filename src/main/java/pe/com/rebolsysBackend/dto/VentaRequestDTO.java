package pe.com.rebolsysBackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.ModoVenta;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VentaRequestDTO {

    @NotBlank(message = "La clave de registro es obligatoria")
    @Size(max = 64, message = "La clave no debe superar 64 caracteres")
    private String claveRegistro;

    @NotNull(message = "Selecciona cómo se cobrará la venta")
    private ModoVenta modoVenta;

    private Long idDetalleReserva;
    private Long idCliente;

    @Size(max = 200, message = "El nombre del comprador no debe superar 200 caracteres")
    private String compradorNombre;

    @NotEmpty(message = "Agrega al menos un producto")
    @Size(max = 100, message = "El carrito admite hasta 100 productos distintos")
    private List<@NotNull @Valid VentaDetalleRequestDTO> detalles;

    @DecimalMin(value = "0", message = "El efectivo no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El efectivo admite 8 dígitos enteros y 2 decimales")
    private BigDecimal efectivoRecibido;

    @JsonIgnore
    @AssertTrue(message = "Selecciona una reserva para el cargo; la venta separada no debe asociarse a una reserva")
    public boolean isDestinoValido() {
        return (
            modoVenta == null ||
            (modoVenta == ModoVenta.CARGO_RESERVA
                ? idDetalleReserva != null && efectivoRecibido == null
                : idDetalleReserva == null)
        );
    }

    @JsonIgnore
    @AssertTrue(message = "Indica el efectivo recibido para una venta separada")
    public boolean isEfectivoValido() {
        return modoVenta != ModoVenta.SEPARADA || efectivoRecibido != null;
    }
}
