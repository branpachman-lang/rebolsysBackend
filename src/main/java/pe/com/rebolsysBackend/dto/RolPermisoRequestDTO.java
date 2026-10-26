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
public class RolPermisoRequestDTO {

    @NotNull(message = "idRol es obligatorio")
    private Long idRol;

    @NotNull(message = "idPermiso es obligatorio")
    private Long idPermiso;
}
