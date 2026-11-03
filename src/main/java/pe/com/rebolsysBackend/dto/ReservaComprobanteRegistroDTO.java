package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;

public record ReservaComprobanteRegistroDTO(
    @NotNull @Positive Long idUsuario,
    @NotNull @Positive Long idTurno,
    @NotNull @Pattern(regexp = "BOLETA|FACTURA") String tipo,
    @NotNull @Positive Long idClienteReceptor,
    @Positive Long idVenta) {}
