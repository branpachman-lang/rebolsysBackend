package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record ItemRequestDTO(
    @NotBlank @Size(max = 100) String nombreItem,
    @NotNull @Positive Long idTipoItem,
    @NotNull EstadoItem estadoItem) {}
