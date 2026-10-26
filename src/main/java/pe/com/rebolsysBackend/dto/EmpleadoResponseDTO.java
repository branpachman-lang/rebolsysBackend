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
public class EmpleadoResponseDTO {

    private Long idEmpleado;

    private Long idPerfil;

    private LocalDate fechaContratacion;

    private LocalDate fechaCese;
}
