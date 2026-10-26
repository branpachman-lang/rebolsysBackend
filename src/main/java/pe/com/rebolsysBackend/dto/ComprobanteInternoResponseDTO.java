package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ComprobanteInternoResponseDTO {

    private String numero;

    private LocalDateTime fecha;

    private String receptor;

    private String documento;

    private List<ComprobanteLineaResponseDTO> lineas;

    private BigDecimal total;

    private BigDecimal pagado;

    private BigDecimal devuelto;
    private String estado;
    private List<PagoDetalleResponseDTO> pagos;
}
