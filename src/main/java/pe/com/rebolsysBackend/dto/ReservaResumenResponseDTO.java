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
public class ReservaResumenResponseDTO {

    private Long idReserva;

    private String referencia;

    private String numero;

    private ClienteResumenResponseDTO cliente;

    private List<ReservaDetalleResponseDTO> habitaciones;

    private String estado;

    private String estadoCobro;

    private BigDecimal total;

    private BigDecimal pagado;

    private BigDecimal saldo;

    private String comprobante;

    private BigDecimal vuelto;

    private BigDecimal devuelto;

    private BigDecimal totalConsumos;
    private List<ConsumoReservaResponseDTO> consumos;
    private List<ComprobanteReferenciaResponseDTO> comprobantes;
}
