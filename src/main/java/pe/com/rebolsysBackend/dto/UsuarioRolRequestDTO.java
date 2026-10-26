package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRolRequestDTO {

    @NotNull(message = "idUsuario es obligatorio")
    private Long idUsuario;

    @NotNull(message = "idRol es obligatorio")
    private Long idRol;
}
