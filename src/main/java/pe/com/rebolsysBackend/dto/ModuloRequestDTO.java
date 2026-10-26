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
public class ModuloRequestDTO {

    @NotNull(message = "nombreModulo es obligatorio")
    @Size(min = 1, max = 50, message = "nombreModulo debe tener entre 1 y 50 caracteres")
    private String nombreModulo;

    @Size(max = 100, message = "ruta no debe superar 100 caracteres")
    private String ruta;
}
