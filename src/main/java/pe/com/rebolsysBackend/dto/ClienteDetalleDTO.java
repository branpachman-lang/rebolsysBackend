package pe.com.rebolsysBackend.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record ClienteDetalleDTO(
    Long idCliente, ClienteRegistroRequestDTO persona, String restriccionesMedicas) {}
