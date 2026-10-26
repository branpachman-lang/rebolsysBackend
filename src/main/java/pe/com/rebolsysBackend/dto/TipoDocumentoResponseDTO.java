package pe.com.rebolsysBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoDocumentoResponseDTO {

    private Long idTipoDocumento;

    private String nombreDocumento;

    private String codigoSunat;
}
