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
public class PermisoRequestDTO {

    @NotNull(message = "idModulo es obligatorio")
    private Long idModulo;

    @NotNull(message = "accion es obligatorio")
    @Size(min = 1, max = 100, message = "accion debe tener entre 1 y 100 caracteres")
    private String accion;
}
