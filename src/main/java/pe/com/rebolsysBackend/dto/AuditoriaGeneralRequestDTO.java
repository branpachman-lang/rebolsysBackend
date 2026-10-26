package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.AccionAuditoria;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaGeneralRequestDTO {

    @NotNull(message = "nombreTabla es obligatorio")
    @Size(min = 1, max = 50, message = "nombreTabla debe tener entre 1 y 50 caracteres")
    private String nombreTabla;

    @NotNull(message = "idRegistro es obligatorio")
    private Long idRegistro;

    @NotNull(message = "accion es obligatorio")
    private AccionAuditoria accion;

    private Long idUsuario;

    private String datosAnteriores;

    private String datosNuevos;

    @Size(max = 45, message = "ipOrigen no debe superar 45 caracteres")
    private String ipOrigen;
}
