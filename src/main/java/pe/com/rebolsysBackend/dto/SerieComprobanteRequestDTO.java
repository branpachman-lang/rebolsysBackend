package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SerieComprobanteRequestDTO {

    @NotNull(message = "idTipoComprobante es obligatorio")
    private Long idTipoComprobante;

    @NotNull(message = "serie es obligatorio")
    @Size(min = 1, max = 10, message = "serie debe tener entre 1 y 10 caracteres")
    private String serie;

    @NotNull(message = "siguienteNumero es obligatorio")
    @Digits(integer = 10, fraction = 0, message = "siguienteNumero admite hasta 10 dígitos enteros y 0 decimales")
    @Min(value = 1, message = "siguienteNumero debe ser mayor o igual a 1")
    @Max(value = 100000000, message = "siguienteNumero no debe superar 100000000")
    private Long siguienteNumero = 1L;
}
