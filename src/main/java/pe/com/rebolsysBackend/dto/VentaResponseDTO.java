package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record VentaResponseDTO(
    Long idVenta,
    Long idClienteComprador,
    String compradorNombreLibre,
    Long idDetalleReserva,
    Long idUsuario,
    ModalidadCobro modalidadCobro,
    EstadoVenta estadoVenta,
    LocalDateTime fechaRegistro,
    LocalDateTime fechaConfirmacion,
    String claveRegistro,
    BigDecimal total,
    List<VentaLineaRespuestaDTO> detalles) {}
