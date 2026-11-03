package pe.com.rebolsysBackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record ReservaDetalleRegistroDTO(
    @NotNull @Positive Long idHabitacion,
    @NotNull LocalDateTime fechaIngreso,
    @NotNull LocalDateTime fechaSalida,
    @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal costoEstadia,
    @Size(max = 99) List<@NotNull @Valid ReservaOcupanteRegistroDTO> ocupantes) {
  public ReservaDetalleRegistroDTO {
    ocupantes = ocupantes == null ? List.of() : ocupantes;
  }
}
