package pe.com.rebolsysBackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GastoResponseDTO(
    Long idFlujo,
    Long idTurno,
    Long idUsuario,
    Long idTipoMovimiento,
    String concepto,
    BigDecimal monto,
    LocalDateTime fechaHora,
    String documentoRespaldo,
    String evidenciaRuta,
    String evidenciaSha256,
    String usuario,
    String tipoGasto,
    boolean turnoAbierto,
    String claveRegistro) {}
