package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class SistemaDTO {
  private SistemaDTO() {}

  public record DatosHospedaje(
      @NotBlank @Size(max = 150) String nombre,
      @NotBlank @Pattern(regexp = "[0-9]{11}") String ruc,
      @Size(max = 200) String direccion) {}

  public record Marca(String nombre, String ruc, String direccion, String logoRuta) {}

  public record Politica(
      @NotNull @Min(1) @Max(1440) Integer minutosInactividad,
      @NotNull @Min(1) @Max(5) Integer intentosFallidos) {}

  public record ParametrosComerciales(
      @NotNull @Min(1) @Max(99) Integer capacidadMax,
      @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal precioBase) {}
}
