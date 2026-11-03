package pe.com.rebolsysBackend.dto;

import java.time.LocalDateTime;

public record TurnoDisponibleDTO(
    Long idTurno, Long idUsuario, String usuario, LocalDateTime fechaApertura) {}
