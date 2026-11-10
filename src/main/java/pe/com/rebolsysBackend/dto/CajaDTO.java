package pe.com.rebolsysBackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class CajaDTO {
  private CajaDTO() {}

  public record Tipo(Long idTipoMovimiento, String nombre, int signo) {}

  public record Arqueo(Long idFormaPago, String formaPago, BigDecimal montoCalculado,
      BigDecimal montoDeclarado, BigDecimal diferencia) {}

  public record Contexto(Long idTurno, LocalDateTime fechaApertura, LocalDateTime fechaCierre,
      BigDecimal montoBase, boolean abierto, List<Tipo> tipos, BigDecimal ingresosManuales,
      BigDecimal gastosManuales, List<Arqueo> arqueos, String revision, String bloqueoCierre) {}

  public record Movimiento(Long idFlujo, String tipo, int signo, String concepto, BigDecimal monto,
      LocalDateTime fechaHora, String documentoRespaldo, String evidencia) {}

  public record RegistrarMovimiento(@NotNull @Positive Long idTipoMovimiento,
      @NotBlank @Size(max = 1000) String concepto,
      @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal monto,
      @NotBlank @Pattern(regexp = "[A-Za-z0-9._:-]{1,64}") String claveRegistro) {}

  public record Declaracion(@NotNull @Positive Long idFormaPago,
      @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal montoDeclarado) {}

  public record Cerrar(@NotBlank @Pattern(regexp = "[a-f0-9]{64}") String revision,
      @NotEmpty List<@NotNull @Valid Declaracion> arqueos) {}
}
