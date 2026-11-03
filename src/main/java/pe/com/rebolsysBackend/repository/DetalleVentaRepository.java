package pe.com.rebolsysBackend.repository;

import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.com.rebolsysBackend.entity.*;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {
  List<DetalleVenta> findByVentaIdVentaOrderByIdDetalleVenta(Long id);
}
