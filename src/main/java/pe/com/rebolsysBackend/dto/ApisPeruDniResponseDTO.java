package pe.com.rebolsysBackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApisPeruDniResponseDTO {
    private String dni;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
}
