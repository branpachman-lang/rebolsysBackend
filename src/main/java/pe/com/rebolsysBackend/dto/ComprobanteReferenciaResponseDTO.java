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
public class ComprobanteReferenciaResponseDTO {

    private Long idComprobante;
    private String numero;
    private String estado;
    private BigDecimal total;
}
