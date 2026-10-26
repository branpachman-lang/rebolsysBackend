package pe.com.rebolsysBackend.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonaNaturalResponseDTO {

    private Long idPerfil;

    private String nombres;

    private String apellidos;

    private LocalDate fechaNacimiento;
}
