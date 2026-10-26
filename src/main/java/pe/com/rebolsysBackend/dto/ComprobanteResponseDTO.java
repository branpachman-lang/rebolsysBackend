package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoComprobante;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteResponseDTO {

    private Long idComprobante;

    private Long idCliente;

    private Long idTipoComprobante;

    private Long idTurno;

    private String serie;

    private String numeroComprobante;

    private LocalDateTime fechaEmision;

    private BigDecimal subtotal;

    private BigDecimal igv;

    private BigDecimal totalPagar;

    private String receptorTipoDoc;

    private String receptorNumeroDoc;

    private String receptorNombre;

    private Long idComprobanteOrigen;

    private EstadoComprobante estadoComprobante;

    private String hashCpe;

    private String sunatCodigoRespuesta;

    private String sunatMensaje;

    private String claveIdempotencia;
}
