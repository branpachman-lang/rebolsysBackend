package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class HabitacionEstadoResponseDTO {

    private Long idHabitacion;

    private String numero;

    private String tipo;

    private int capacidad;
    private int plazasCama;
    private int maxMenoresSinCama;

    private BigDecimal precio;

    private String estado;

    private boolean disponible;

    private List<HabitacionReservaResponseDTO> reservas;
}
