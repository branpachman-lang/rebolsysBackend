package pe.com.rebolsysBackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoComprobante;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteRequestDTO {

    private Long idCliente;

    @NotNull(message = "idTipoComprobante es obligatorio")
    private Long idTipoComprobante;

    @NotNull(message = "idTurno es obligatorio")
    private Long idTurno;

    @NotNull(message = "serie es obligatorio")
    @Size(min = 1, max = 10, message = "serie debe tener entre 1 y 10 caracteres")
    private String serie;

    @Size(max = 20, message = "numeroComprobante no debe superar 20 caracteres")
    private String numeroComprobante;

    @NotNull(message = "subtotal es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "subtotal admite hasta 10 dígitos enteros y 2 decimales")
    @PositiveOrZero(message = "subtotal debe ser mayor o igual a 0")
    private BigDecimal subtotal;

    @NotNull(message = "igv es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "igv admite hasta 10 dígitos enteros y 2 decimales")
    @PositiveOrZero(message = "igv debe ser mayor o igual a 0")
    private BigDecimal igv;

    @NotNull(message = "totalPagar es obligatorio")
    @Digits(integer = 10, fraction = 2, message = "totalPagar admite hasta 10 dígitos enteros y 2 decimales")
    @PositiveOrZero(message = "totalPagar debe ser mayor o igual a 0")
    private BigDecimal totalPagar;

    @Size(max = 10, message = "receptorTipoDoc no debe superar 10 caracteres")
    private String receptorTipoDoc;

    @Size(max = 20, message = "receptorNumeroDoc no debe superar 20 caracteres")
    private String receptorNumeroDoc;

    @NotNull(message = "receptorNombre es obligatorio")
    @Size(min = 1, max = 200, message = "receptorNombre debe tener entre 1 y 200 caracteres")
    private String receptorNombre;

    private Long idComprobanteOrigen;

    @NotNull(message = "estadoComprobante es obligatorio")
    private EstadoComprobante estadoComprobante;

    @Size(max = 100, message = "hashCpe no debe superar 100 caracteres")
    private String hashCpe;

    @Size(max = 10, message = "sunatCodigoRespuesta no debe superar 10 caracteres")
    private String sunatCodigoRespuesta;

    @Size(max = 500, message = "sunatMensaje no debe superar 500 caracteres")
    private String sunatMensaje;

    @Size(max = 64, message = "claveIdempotencia no debe superar 64 caracteres")
    private String claveIdempotencia;

    @JsonIgnore
    @AssertTrue(message = "El total a pagar debe ser igual a subtotal más IGV")
    public boolean isTotalValido() {
        return subtotal == null || igv == null || totalPagar == null || totalPagar.compareTo(subtotal.add(igv)) == 0;
    }
}
