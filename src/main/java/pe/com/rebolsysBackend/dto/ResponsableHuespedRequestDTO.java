package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.ParentescoResponsable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ResponsableHuespedRequestDTO {

    @NotNull(message = "El tipo de documento del responsable es obligatorio")
    private Long idTipoDocumento;

    @NotBlank(message = "El documento del responsable es obligatorio")
    @Size(max = 50, message = "El documento del responsable no debe superar 50 caracteres")
    private String numeroDocumento;

    @NotNull(message = "Selecciona si el responsable es padre, madre o tutor")
    private ParentescoResponsable parentesco;
}
