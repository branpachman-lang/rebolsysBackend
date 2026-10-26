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
public class PerfilRequestDTO {

    @NotNull(message = "idTipoDocumento es obligatorio")
    private Long idTipoDocumento;

    @NotNull(message = "numeroDocumento es obligatorio")
    @Size(min = 1, max = 50, message = "numeroDocumento debe tener entre 1 y 50 caracteres")
    private String numeroDocumento;

    @Size(max = 100, message = "correo no debe superar 100 caracteres")
    private String correo;

    @Size(max = 15, message = "telefono no debe superar 15 caracteres")
    private String telefono;
}
