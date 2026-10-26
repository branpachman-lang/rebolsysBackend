package pe.com.rebolsysBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoConservacion;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleInformeResponseDTO {

    private Long idDetalleInforme;

    private Long idInforme;

    private Long idDetalleHabitacion;

    private EstadoConservacion estadoEncontrado;

    private String evidenciaRuta;

    private String evidenciaSha256;
}
