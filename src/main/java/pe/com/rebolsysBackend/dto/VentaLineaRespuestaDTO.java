package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record VentaLineaRespuestaDTO(
    Long idDetalleVenta,
    Long idProducto,
    String descripcion,
    int cantidad,
    BigDecimal valorUnitario,
    BigDecimal igv,
    BigDecimal subtotal,
    BigDecimal total,
    String estadoLinea) {}
