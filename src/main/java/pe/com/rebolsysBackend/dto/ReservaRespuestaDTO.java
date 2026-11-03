package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record ReservaRespuestaDTO(
    Long idReserva,
    Long idCliente,
    Long idUsuario,
    String titular,
    LocalDateTime fechaRegistro,
    String claveRegistro,
    List<ReservaDetalleRespuestaDTO> detalles) {}
