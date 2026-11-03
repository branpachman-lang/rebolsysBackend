package pe.com.rebolsysBackend.repository;

import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.com.rebolsysBackend.entity.*;

public interface VentaRepository extends JpaRepository<Venta, Long> {
  List<Venta> findByDetalleReservaReservaIdReservaOrderByIdVenta(Long id);

  Optional<Venta> findByClaveRegistro(String clave);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select v from Venta v where v.idVenta=:id")
  Optional<Venta> bloquear(@Param("id") Long id);
}
