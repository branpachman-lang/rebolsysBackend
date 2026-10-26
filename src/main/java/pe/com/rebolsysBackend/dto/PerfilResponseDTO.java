package pe.com.rebolsysBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerfilResponseDTO {

    private Long idPerfil;

    private Long idTipoDocumento;

    private String numeroDocumento;

    private String correo;

    private String telefono;
}
