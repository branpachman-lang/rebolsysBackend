package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import pe.com.rebolsysBackend.enums.EstadoConservacion;

public record ItemConservacionDTO(
    @NotBlank @Size(max = 64) String claveOperacion,
    @NotNull @Positive Long idUsuario,
    @NotNull EstadoConservacion estadoActualItem,
    @NotBlank @Size(max = 180) String observacion) {}
