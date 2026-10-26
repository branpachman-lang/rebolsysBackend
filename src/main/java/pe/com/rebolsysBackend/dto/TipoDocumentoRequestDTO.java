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
public class TipoDocumentoRequestDTO {

    @NotNull(message = "nombreDocumento es obligatorio")
    @Size(min = 1, max = 30, message = "nombreDocumento debe tener entre 1 y 30 caracteres")
    private String nombreDocumento;

    @Size(max = 2, message = "codigoSunat no debe superar 2 caracteres")
    private String codigoSunat;
}
