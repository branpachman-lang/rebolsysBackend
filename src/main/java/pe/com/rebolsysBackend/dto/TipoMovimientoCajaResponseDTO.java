package pe.com.rebolsysBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoMovimientoCajaResponseDTO {

    private Long idTipoMovimiento;

    private String nombreMovimiento;

    private Integer signo;
}
