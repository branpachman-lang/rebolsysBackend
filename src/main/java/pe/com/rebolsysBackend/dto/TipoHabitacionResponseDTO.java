package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoHabitacionResponseDTO {

    private Long idTipoHabitacion;

    private String nombreTipo;

    private Integer capacidadMax;

    private BigDecimal precioBase;
}
