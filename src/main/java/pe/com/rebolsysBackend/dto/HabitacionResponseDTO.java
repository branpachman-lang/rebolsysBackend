package pe.com.rebolsysBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoHabitacion;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HabitacionResponseDTO {

    private Long idHabitacion;

    private String numeroCuarto;

    private Long idTipo;

    private EstadoHabitacion estadoHabitacion;
    private Integer capacidadPersonas;
    private Integer plazasCama;
    private Integer maxMenoresSinCama;
}
