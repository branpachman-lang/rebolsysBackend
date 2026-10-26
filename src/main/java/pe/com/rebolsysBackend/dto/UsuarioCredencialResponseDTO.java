package pe.com.rebolsysBackend.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioCredencialResponseDTO {

    private Long idCredencial;

    private Long idUsuario;

    private Long idTipoCredencial;

    private Integer credencialTemporal;

    private Integer intentosFallidos;

    private LocalDateTime fechaBloqueo;
}
