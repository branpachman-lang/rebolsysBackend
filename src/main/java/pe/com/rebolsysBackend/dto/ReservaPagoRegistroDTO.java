package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ReservaPagoRegistroDTO(
    @NotBlank @Size(max = 64) String claveOperacion,
    @NotNull @Positive Long idUsuario,
    @NotNull @Positive Long idTurno,
    @NotNull @Positive Long idFormaPago,
    @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal monto,
    @Size(max = 50) String numeroOperacion,
    @Positive Long idVenta) {}
