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
public class DetalleHabitacionResponseDTO {

    private Long idDetalleHabitacion;

    private Long idHabitacion;

    private Long idItem;

    private EstadoConservacion estadoActualItem;
}
