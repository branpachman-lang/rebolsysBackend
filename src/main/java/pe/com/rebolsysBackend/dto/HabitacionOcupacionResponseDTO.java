package pe.com.rebolsysBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HabitacionOcupacionResponseDTO {

    private Long idHabitacion;
    private Integer capacidadPersonas;
    private Integer plazasCama;
    private Integer maxMenoresSinCama;
}
