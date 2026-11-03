package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;

public record CuentaLineaDTO(
    Long idDetalleReserva,
    Long idDetalleVenta,
    String descripcion,
    BigDecimal cantidad,
    BigDecimal total) {}
