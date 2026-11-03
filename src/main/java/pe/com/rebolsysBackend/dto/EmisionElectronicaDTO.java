package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import pe.com.rebolsysBackend.enums.EstadoEmisionElectronica;

public record EmisionElectronicaDTO(
    Long idEmision, Long idComprobante, Long idUsuario, String ambiente,
    EstadoEmisionElectronica estado, String rucEmisor, String tipoDocumento,
    String serie, Long correlativo, LocalDateTime fechaEmision,
    LocalDateTime fechaActualizacion, int intentos, BigDecimal subtotal,
    BigDecimal igv, BigDecimal total, String codigoRespuesta, String mensajeRespuesta,
    boolean xmlDisponible, boolean cdrDisponible) {}
