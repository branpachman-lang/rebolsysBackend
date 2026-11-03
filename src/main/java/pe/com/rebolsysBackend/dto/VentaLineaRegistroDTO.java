package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record VentaLineaRegistroDTO(
    @NotNull @Positive Long idProducto, @NotNull @Min(1) @Max(999999999) Integer cantidad) {}
