package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonaNaturalRequestDTO {

    @NotNull(message = "idPerfil es obligatorio")
    private Long idPerfil;

    @NotNull(message = "nombres es obligatorio")
    @Size(min = 1, max = 100, message = "nombres debe tener entre 1 y 100 caracteres")
    private String nombres;

    @NotNull(message = "apellidos es obligatorio")
    @Size(min = 1, max = 100, message = "apellidos debe tener entre 1 y 100 caracteres")
    private String apellidos;

    @PastOrPresent(message = "La fecha de nacimiento no puede ser futura")
    private LocalDate fechaNacimiento;
}
