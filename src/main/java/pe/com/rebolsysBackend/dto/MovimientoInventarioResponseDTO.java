package pe.com.rebolsysBackend.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoInventarioResponseDTO {

    private Long idMovimiento;

    private Long idProducto;

    private Long idItem;

    private Long idConsumo;

    private Long idDetalleHabitacion;

    private Long idTipoMovimientoAlmacen;

    private Integer cantidad;

    private Integer stockResultante;

    private Long idUsuario;

    private String observacion;

    private LocalDateTime fechaMovimiento;
}
