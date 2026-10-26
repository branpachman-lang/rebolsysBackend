package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoCargo;
import pe.com.rebolsysBackend.enums.EstadoCobro;
import pe.com.rebolsysBackend.enums.TipoConsumo;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsumoResponseDTO {

    private Long idConsumo;

    private Long idCliente;

    private Long idDetalleReserva;

    private Long idProducto;

    private Long idUsuario;

    private TipoConsumo tipoConsumo;

    private String descripcion;

    private BigDecimal cantidad;

    private BigDecimal valorUnitario;

    private BigDecimal subtotal;

    private BigDecimal igv;

    private BigDecimal total;

    private EstadoCargo estadoCargo;

    private EstadoCobro estadoCobro;

    private LocalDateTime fechaConsumo;

    private Long idVenta;
}
