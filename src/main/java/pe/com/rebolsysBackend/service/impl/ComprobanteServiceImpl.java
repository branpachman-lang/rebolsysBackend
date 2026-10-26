package pe.com.rebolsysBackend.service.impl;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.ComprobanteInternoResponseDTO;
import pe.com.rebolsysBackend.dto.ComprobanteLineaResponseDTO;
import pe.com.rebolsysBackend.dto.PagoDetalleResponseDTO;
import pe.com.rebolsysBackend.entity.Comprobante;
import pe.com.rebolsysBackend.entity.DetalleReserva;
import pe.com.rebolsysBackend.entity.FlujoCaja;
import pe.com.rebolsysBackend.entity.Pago;
import pe.com.rebolsysBackend.exception.RecursoNoEncontradoException;
import pe.com.rebolsysBackend.repository.ComprobanteDetalleRepository;
import pe.com.rebolsysBackend.repository.ComprobanteRepository;
import pe.com.rebolsysBackend.repository.FlujoCajaRepository;
import pe.com.rebolsysBackend.repository.PagoRepository;
import pe.com.rebolsysBackend.service.service.ComprobanteService;

@Service
@RequiredArgsConstructor
public class ComprobanteServiceImpl implements ComprobanteService {

    private final ComprobanteRepository comprobantes;
    private final ComprobanteDetalleRepository detalles;
    private final PagoRepository pagos;
    private final FlujoCajaRepository flujos;

    @Override
    @Transactional(readOnly = true)
    public ComprobanteInternoResponseDTO obtener(List<DetalleReserva> rows) {
        var found = recibos(rows);
        if (found.isEmpty()) throw new RecursoNoEncontradoException(
            "La reserva todavía no tiene comprobante. Registra el pago en efectivo."
        );
        return obtenerPorId(
            found
                .stream()
                .max(java.util.Comparator.comparing(Comprobante::getIdComprobante))
                .orElseThrow()
                .getIdComprobante()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ComprobanteInternoResponseDTO obtenerPorId(Long id) {
        var receipt = comprobantes
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("El comprobante no existe"));
        var details = detalles.findByComprobanteOrderByNumeroLinea(receipt);
        BigDecimal paid = pagos
            .findByComprobanteIdComprobante(receipt.getIdComprobante())
            .stream()
            .map(Pago::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ComprobanteInternoResponseDTO(
            receipt.getSerie() + "-" + receipt.getNumeroComprobante(),
            receipt.getFechaEmision(),
            receipt.getReceptorNombre(),
            receipt.getReceptorNumeroDoc(),
            details
                .stream()
                .map(d ->
                    new ComprobanteLineaResponseDTO(
                        d.getDescripcion(),
                        d.getCantidad(),
                        d.getValorUnitario(),
                        d.getTotal()
                    )
                )
                .toList(),
            receipt.getTotalPagar(),
            paid,
            pagos
                .findByComprobanteIdComprobante(receipt.getIdComprobante())
                .stream()
                .map(this::devolucionPago)
                .reduce(BigDecimal.ZERO, BigDecimal::add),
            receipt.getEstadoComprobante().name(),
            pagos
                .findByComprobanteIdComprobante(receipt.getIdComprobante())
                .stream()
                .map(p -> new PagoDetalleResponseDTO(p.getIdPago(), p.getFechaPago(), p.getMonto(), devolucionPago(p)))
                .toList()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comprobante> recibos(List<DetalleReserva> rows) {
        return comprobantes.buscarPorReservas(rows);
    }

    private BigDecimal devolucionPago(Pago pago) {
        return flujos
            .findByPagoIdPagoAndTipoMovimientoCajaSigno(pago.getIdPago(), -1)
            .stream()
            .map(FlujoCaja::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal devuelto(List<DetalleReserva> rows) {
        return recibos(rows)
            .stream()
            .flatMap(c -> pagos.findByComprobanteIdComprobante(c.getIdComprobante()).stream())
            .map(this::devolucionPago)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal pagado(List<DetalleReserva> reservas) {
        return recibos(reservas)
            .stream()
            .flatMap(comprobante -> pagos.findByComprobanteIdComprobante(comprobante.getIdComprobante()).stream())
            .map(Pago::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
