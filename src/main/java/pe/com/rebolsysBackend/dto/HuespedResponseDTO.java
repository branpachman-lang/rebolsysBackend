package pe.com.rebolsysBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HuespedResponseDTO {

    private Long idHuesped;

    private Long idPerfil;

    private String restriccionesMedicas;
}
