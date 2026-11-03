package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;
import java.util.List;

public record CuentaReservaDTO(
    Long idReserva,
    String numeroReserva,
    BigDecimal totalAlojamiento,
    BigDecimal totalCargos,
    BigDecimal totalPagado,
    BigDecimal saldoPendiente,
    boolean admiteConsumosConsolidados,
    List<CuentaDocumentoDTO> cuentas) {}
