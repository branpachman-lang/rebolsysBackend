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
public class HuespedRequestDTO {

    @NotNull(message = "idPerfil es obligatorio")
    private Long idPerfil;

    @Size(max = 500, message = "restriccionesMedicas no debe superar 500 caracteres")
    private String restriccionesMedicas;
}
