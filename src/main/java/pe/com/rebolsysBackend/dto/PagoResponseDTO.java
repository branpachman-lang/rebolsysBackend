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
public class PagoResponseDTO {

    private Long idPago;

    private Long idComprobante;

    private Long idFormaPago;

    private Long idUsuario;

    private Long idTurno;

    private BigDecimal monto;

    private String numeroOperacion;

    private LocalDateTime fechaPago;

    private Integer verificado;

    private String claveIdempotencia;
}
