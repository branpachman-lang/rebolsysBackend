package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import pe.com.rebolsysBackend.enums.EstadoComprobante;

public record CuentaDocumentoDTO(
    Long idCuenta,
    Long idReserva,
    String numeroReserva,
    Long idVenta,
    String tipo,
    EstadoComprobante estado,
    String serie,
    String numero,
    Long idClienteReceptor,
    String receptorNombre,
    String receptorNumeroDoc,
    LocalDateTime fechaEmision,
    BigDecimal total,
    BigDecimal pagado,
    BigDecimal saldo,
    List<CuentaLineaDTO> lineas,
    List<ReservaPagoRespuestaDTO> pagos) {}
