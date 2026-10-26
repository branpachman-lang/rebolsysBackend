package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ComprobanteLineaResponseDTO {

    private String descripcion;

    private BigDecimal cantidad;

    private BigDecimal valorUnitario;

    private BigDecimal total;
}
