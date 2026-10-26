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
public class PersonaJuridicaRequestDTO {

    @NotNull(message = "idPerfil es obligatorio")
    private Long idPerfil;

    @NotNull(message = "razonSocial es obligatorio")
    @Size(min = 1, max = 250, message = "razonSocial debe tener entre 1 y 250 caracteres")
    private String razonSocial;

    @Size(max = 200, message = "direccionFiscal no debe superar 200 caracteres")
    private String direccionFiscal;
}
