package pe.com.rebolsysBackend.repository;

import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.com.rebolsysBackend.entity.*;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {
  boolean existsByVentaIdVenta(Long id);

  Optional<Comprobante> findByClaveIdempotencia(String clave);

  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Comprobante c where c.idComprobante=:id")
  Optional<Comprobante> bloquear(@org.springframework.data.repository.query.Param("id") Long id);

  @Query(
      "select c from Comprobante c left join c.venta v left join v.detalleReserva d where"
          + " (c.reserva.idReserva=:id or d.reserva.idReserva=:id) and"
          + " c.estadoComprobante<>pe.com.rebolsysBackend.enums.EstadoComprobante.ANULADO order by"
          + " c.idComprobante")
  List<Comprobante> cuentasReserva(@org.springframework.data.repository.query.Param("id") Long id);
}
