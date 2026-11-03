package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record ReservaOcupanteRegistroDTO(
    @NotNull @Positive Long idCliente,
    @Positive Long idClienteResponsable,
    ParentescoResponsable parentescoResponsable,
    @NotNull Boolean ocupaPlazaCama) {}
