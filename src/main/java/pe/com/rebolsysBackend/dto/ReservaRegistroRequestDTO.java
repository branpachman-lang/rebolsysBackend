package pe.com.rebolsysBackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record ReservaRegistroRequestDTO(
    @NotBlank @Size(max = 64) String claveRegistro,
    @NotNull @Positive Long idCliente,
    @NotNull @Positive Long idUsuario,
    @NotEmpty @Size(max = 50) List<@NotNull @Valid ReservaDetalleRegistroDTO> detalles) {}
