package pe.com.rebolsysBackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record VentaRequestDTO(
    @NotBlank @Size(max = 64) String claveRegistro,
    @NotNull @Positive Long idUsuario,
    @Positive Long idClienteComprador,
    @Size(max = 250) String compradorNombreLibre,
    @NotNull ModalidadCobro modalidadCobro,
    @Positive Long idDetalleReserva,
    @NotEmpty @Size(max = 100) List<@NotNull @Valid VentaLineaRegistroDTO> detalles) {}
