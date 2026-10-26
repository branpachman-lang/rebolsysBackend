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
public class ArqueoCajaResponseDTO {

    private Long idArqueo;

    private Long idTurno;

    private Long idFormaPago;

    private BigDecimal montoCalculado;

    private BigDecimal montoDeclarado;

    private BigDecimal diferencia;
}
