package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;

public record ComprobanteCuentaDTO(
    @NotNull @Positive Long idUsuario,
    @NotNull @Positive Long idTurno,
    @NotNull @Pattern(regexp = "BOLETA|FACTURA") String tipo,
    @NotNull @Positive Long idClienteReceptor) {}
