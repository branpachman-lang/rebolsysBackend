package pe.com.rebolsysBackend.dto;

public record ConsumoReservaRespuestaDTO(
    Long idReserva, String numeroReserva, Long idCuenta, VentaResponseDTO venta) {}
