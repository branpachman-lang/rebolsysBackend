package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoUsuario;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRequestDTO {

    @NotNull(message = "idEmpleado es obligatorio")
    private Long idEmpleado;

    @NotNull(message = "username es obligatorio")
    @Size(min = 1, max = 50, message = "username debe tener entre 1 y 50 caracteres")
    private String username;

    @NotNull(message = "estadoUsuario es obligatorio")
    private EstadoUsuario estadoUsuario;
}
