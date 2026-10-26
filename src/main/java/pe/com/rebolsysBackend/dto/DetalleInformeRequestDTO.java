package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoConservacion;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleInformeRequestDTO {

    @NotNull(message = "idInforme es obligatorio")
    private Long idInforme;

    @NotNull(message = "idDetalleHabitacion es obligatorio")
    private Long idDetalleHabitacion;

    @NotNull(message = "estadoEncontrado es obligatorio")
    private EstadoConservacion estadoEncontrado;

    @Size(max = 300, message = "evidenciaRuta no debe superar 300 caracteres")
    private String evidenciaRuta;

    @Size(min = 64, max = 64, message = "evidenciaSha256 debe tener entre 64 y 64 caracteres")
    private String evidenciaSha256;
}
