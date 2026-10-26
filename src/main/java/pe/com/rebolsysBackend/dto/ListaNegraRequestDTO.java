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
public class ListaNegraRequestDTO {

    @NotNull(message = "idPerfil es obligatorio")
    private Long idPerfil;

    @NotNull(message = "motivo es obligatorio")
    @Size(min = 1, max = 500, message = "motivo debe tener entre 1 y 500 caracteres")
    private String motivo;

    @NotNull(message = "idUsuario es obligatorio")
    private Long idUsuario;
}
