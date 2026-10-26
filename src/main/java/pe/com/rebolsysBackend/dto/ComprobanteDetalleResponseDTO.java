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
public class ComprobanteDetalleResponseDTO {

    private Long idComprobanteDetalle;

    private Long idComprobante;

    private Long idConsumo;

    private Integer numeroLinea;

    private String descripcion;

    private BigDecimal cantidad;

    private BigDecimal valorUnitario;

    private BigDecimal subtotal;

    private BigDecimal igv;

    private BigDecimal total;

    private Long idHabitacion;
}
