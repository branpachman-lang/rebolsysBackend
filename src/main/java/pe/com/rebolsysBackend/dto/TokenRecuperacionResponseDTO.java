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
public class TokenRecuperacionResponseDTO {

    private Long idToken;

    private Long idUsuario;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaExpiracion;

    private Integer usado;
}
