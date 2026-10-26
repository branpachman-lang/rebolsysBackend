package pe.com.rebolsysBackend.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.AccionAuditoria;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaGeneralResponseDTO {

    private Long idAuditoria;

    private String nombreTabla;

    private Long idRegistro;

    private AccionAuditoria accion;

    private Long idUsuario;

    private String datosAnteriores;

    private String datosNuevos;

    private String ipOrigen;

    private LocalDateTime fechaHora;
}
