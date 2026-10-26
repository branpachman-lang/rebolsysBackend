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
public class ParametroSistemaRequestDTO {

    @NotNull(message = "clave es obligatorio")
    @Size(min = 1, max = 50, message = "clave debe tener entre 1 y 50 caracteres")
    private String clave;

    @NotNull(message = "valor es obligatorio")
    @Size(min = 1, max = 200, message = "valor debe tener entre 1 y 200 caracteres")
    private String valor;

    @Size(max = 200, message = "descripcion no debe superar 200 caracteres")
    private String descripcion;
}
