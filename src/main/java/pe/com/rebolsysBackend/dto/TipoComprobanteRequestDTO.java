package pe.com.rebolsysBackend.dto;

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
public class TipoComprobanteRequestDTO {

    @NotNull(message = "codigo es obligatorio")
    @Size(min = 1, max = 20, message = "codigo debe tener entre 1 y 20 caracteres")
    private String codigo;

    @NotNull(message = "nombreComprobante es obligatorio")
    @Size(min = 1, max = 30, message = "nombreComprobante debe tener entre 1 y 30 caracteres")
    private String nombreComprobante;

    @Size(max = 2, message = "codigoSunat no debe superar 2 caracteres")
    private String codigoSunat;
}
