package pe.com.rebolsysBackend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.rebolsysBackend.entity.Comprobante;
import pe.com.rebolsysBackend.entity.ComprobanteDetalle;

public interface ComprobanteDetalleRepository extends JpaRepository<ComprobanteDetalle, Long> {
    List<ComprobanteDetalle> findByComprobanteOrderByNumeroLinea(Comprobante comprobante);
}
