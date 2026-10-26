package pe.com.rebolsysBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class DocumentoConsultaResponseDTO {

    private String numeroDocumento;
    private String nombres;
    private String apellidos;
    private String razonSocial;
    private String direccionFiscal;
}
