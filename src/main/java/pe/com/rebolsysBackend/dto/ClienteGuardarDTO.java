package pe.com.rebolsysBackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import pe.com.rebolsysBackend.enums.*;

public record ClienteGuardarDTO(
    @NotNull @Valid ClienteRegistroRequestDTO persona,
    @Size(max = 500) String restriccionesMedicas) {}
