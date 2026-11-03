package pe.com.rebolsysBackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EmisionElectronicaRequestDTO(
    @NotBlank @Size(max=64) String claveOperacion,
    @NotNull @Positive Long idUsuario,
    @NotNull @DecimalMin("0.01") @DecimalMax("100") @Digits(integer=3, fraction=2) BigDecimal tasaIgv,
    @Size(max=200) String direccionReceptor,
    @NotNull @Size(max=500) List<@NotNull @Valid TratamientoLinea> tratamientos,
    @NotNull @Size(max=120) List<@NotNull @Valid Cuota> cuotas) {
  public EmisionElectronicaRequestDTO {
    tratamientos = tratamientos == null ? List.of() : List.copyOf(tratamientos);
    cuotas = cuotas == null ? List.of() : List.copyOf(cuotas);
  }

  public record TratamientoLinea(
      @NotNull @Positive Integer numeroLinea,
      @NotNull @Pattern(regexp="10|20|30") String afectacion,
      @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3, fraction=2) BigDecimal tasaIgv) {}

  public record Cuota(
      @NotNull @FutureOrPresent LocalDate fechaPago,
      @NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal monto) {}
}
