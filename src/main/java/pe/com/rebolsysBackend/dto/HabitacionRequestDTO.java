package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record HabitacionRequestDTO(
    @NotBlank @Size(max = 10) String numeroCuarto,
    @NotNull @Positive Long idTipoHabitacion,
    @NotNull EstadoHabitacion estadoHabitacion,
    @NotNull @Min(1) @Max(99) Integer capacidadPersonas,
    @NotNull @Min(1) @Max(99) Integer plazasCama,
    @NotNull @Min(0) @Max(value=1,message="Solo se permite un menor adicional sin cama por habitación") Integer maxMenoresSinCama,
    @io.swagger.v3.oas.annotations.media.Schema(description = "Tarifa de la habitación con impuesto incluido. Si se omite al editar, se conserva la tarifa actual. Al crear sin tarifa propia se usa la del tipo.")
    @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal precioPorNoche) {

  public HabitacionRequestDTO(String numeroCuarto, Long idTipoHabitacion,
      EstadoHabitacion estadoHabitacion, Integer capacidadPersonas, Integer plazasCama,
      Integer maxMenoresSinCama) {
    this(numeroCuarto, idTipoHabitacion, estadoHabitacion, capacidadPersonas,
        plazasCama, maxMenoresSinCama, null);
  }
}
