package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record ReservaOcupanteRespuestaDTO(
    Long idReservaOcupante,
    Long idCliente,
    String nombre,
    Long idOcupanteResponsable,
    ParentescoResponsable parentescoResponsable,
    boolean ocupaPlazaCama) {}
