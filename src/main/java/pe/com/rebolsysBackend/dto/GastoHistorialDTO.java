package pe.com.rebolsysBackend.dto;

public record GastoHistorialDTO(
    PaginaResponseDTO<GastoResponseDTO> gastos, GastoResumenDTO resumen) {}
