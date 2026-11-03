package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record ReservaDetalleRespuestaDTO(
    Long idDetalleReserva,
    Long idHabitacion,
    String numeroCuarto,
    LocalDateTime fechaIngreso,
    LocalDateTime fechaSalida,
    LocalDateTime fechaIngresoReal,
    LocalDateTime fechaSalidaReal,
    BigDecimal costoEstadia,
    EstadoReserva estadoDetalle,
    List<ReservaOcupanteRespuestaDTO> ocupantes) {}
