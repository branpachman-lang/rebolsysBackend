package pe.com.rebolsysBackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record VentaEditarDTO(
    @NotNull @Positive Long idUsuario,
    @Positive Long idClienteComprador,
    @Size(max = 250) String compradorNombreLibre,
    @NotEmpty @Size(max = 100) List<@NotNull @Valid VentaLineaRegistroDTO> detalles) {}
