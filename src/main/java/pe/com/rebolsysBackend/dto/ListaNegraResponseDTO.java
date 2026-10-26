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
public class ListaNegraResponseDTO {

    private Long idListaNegra;

    private Long idPerfil;

    private String motivo;

    private LocalDateTime fechaRegistro;

    private Long idUsuario;
}
