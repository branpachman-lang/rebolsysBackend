package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record ItemResponseDTO(
    Long idItem,
    String nombreItem,
    Long idTipoItem,
    EstadoItem estadoItem,
    Long idHabitacion,
    EstadoConservacion estadoActualItem,
    String nombreTipo,
    String numeroCuarto) {}
