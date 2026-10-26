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
public class FlujoCajaResponseDTO {

    private Long idFlujo;

    private Long idTurno;

    private Long idTipoMovimiento;

    private Long idPago;

    private String concepto;

    private BigDecimal monto;

    private LocalDateTime fechaHora;

    private String documentoRespaldo;

    private String evidenciaRuta;

    private String evidenciaSha256;
}
