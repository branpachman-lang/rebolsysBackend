package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record AjusteStockDTO(
        @NotBlank @Size(max = 64) String claveOperacion,
        @NotNull @Positive Long idUsuario,
        @NotNull @Min(0) @Max(999999999) Integer stockAnterior,
        @NotNull @Min(0) @Max(999999999) Integer stockNuevo,
        @NotBlank @Size(max = 300) String observacion) {
}
