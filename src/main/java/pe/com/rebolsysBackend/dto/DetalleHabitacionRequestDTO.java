package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoConservacion;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleHabitacionRequestDTO {

    @NotNull(message = "idHabitacion es obligatorio")
    private Long idHabitacion;

    @NotNull(message = "idItem es obligatorio")
    private Long idItem;

    @NotNull(message = "estadoActualItem es obligatorio")
    private EstadoConservacion estadoActualItem;
}
