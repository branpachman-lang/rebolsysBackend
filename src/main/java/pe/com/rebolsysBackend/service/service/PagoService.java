package pe.com.rebolsysBackend.service.service;

import java.math.BigDecimal;
import java.util.List;
import pe.com.rebolsysBackend.entity.DetalleReserva;

public interface PagoService {
    BigDecimal cobrar(List<DetalleReserva> reservas, String clave, BigDecimal efectivoRecibido, BigDecimal montoPago);
    pe.com.rebolsysBackend.entity.Comprobante cobrarVenta(
        java.util.List<pe.com.rebolsysBackend.entity.Consumo> consumos,
        pe.com.rebolsysBackend.entity.Cliente cliente,
        pe.com.rebolsysBackend.entity.Usuario usuario,
        String compradorNombre,
        String clave,
        BigDecimal recibido
    );
    void devolver(List<DetalleReserva> reservas, String motivo);
}
