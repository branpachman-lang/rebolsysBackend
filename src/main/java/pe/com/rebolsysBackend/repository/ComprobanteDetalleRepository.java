package pe.com.rebolsysBackend.repository;

import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.com.rebolsysBackend.entity.*;

public interface ComprobanteDetalleRepository extends JpaRepository<ComprobanteDetalle, Long> {
  List<ComprobanteDetalle> findByComprobanteIdComprobanteOrderByNumeroLinea(Long id);

  boolean existsByDetalleVentaVentaIdVenta(Long id);

  boolean existsByComprobanteIdComprobanteAndDetalleVentaIdDetalleVenta(Long cuenta, Long linea);

  List<ComprobanteDetalle> findByDetalleReservaIdDetalleReserva(Long id);
}
