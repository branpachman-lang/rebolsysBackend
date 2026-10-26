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
public class TipoCredencialRequestDTO {

    @NotNull(message = "nombreCredencial es obligatorio")
    @Size(min = 1, max = 50, message = "nombreCredencial debe tener entre 1 y 50 caracteres")
    private String nombreCredencial;
}
