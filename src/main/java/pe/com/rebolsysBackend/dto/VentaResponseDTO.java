package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.ModoVenta;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VentaResponseDTO {

    private Long idVenta;
    private String numero;
    private LocalDateTime fechaVenta;
    private ModoVenta modoVenta;
    private String compradorNombre;
    private Long idDetalleReserva;
    private Long idReserva;
    private String referenciaReserva;
    private String habitacion;
    private String estadoCobro;
    private BigDecimal subtotal;
    private BigDecimal igv;
    private BigDecimal total;
    private BigDecimal vuelto;
    private List<VentaDetalleResponseDTO> detalles;
    private ComprobanteInternoResponseDTO comprobante;
    private boolean editable;
}
