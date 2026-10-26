package pe.com.rebolsysBackend.service.service;

import java.math.BigDecimal;
import java.util.List;
import pe.com.rebolsysBackend.dto.ComprobanteInternoResponseDTO;
import pe.com.rebolsysBackend.entity.Comprobante;
import pe.com.rebolsysBackend.entity.DetalleReserva;

public interface ComprobanteService {
    ComprobanteInternoResponseDTO obtener(List<DetalleReserva> reservas);
    ComprobanteInternoResponseDTO obtenerPorId(Long id);
    List<Comprobante> recibos(List<DetalleReserva> reservas);
    BigDecimal pagado(List<DetalleReserva> reservas);
    BigDecimal devuelto(List<DetalleReserva> reservas);
}
