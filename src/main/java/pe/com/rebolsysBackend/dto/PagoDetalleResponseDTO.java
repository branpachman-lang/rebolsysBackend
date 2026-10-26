package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagoDetalleResponseDTO {

    private Long idPago;
    private LocalDateTime fecha;
    private BigDecimal monto;
    private BigDecimal devuelto;
}
