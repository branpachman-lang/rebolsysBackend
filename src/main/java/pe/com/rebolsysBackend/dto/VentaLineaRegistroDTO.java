package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record VentaLineaRegistroDTO(
    @NotNull @Positive Long idProducto, @NotNull @Min(1) @Max(999999999) Integer cantidad,
    @PositiveOrZero @Digits(integer=8,fraction=2) java.math.BigDecimal precioEsperado) {
  public VentaLineaRegistroDTO(Long idProducto,Integer cantidad){this(idProducto,cantidad,null);}
}
