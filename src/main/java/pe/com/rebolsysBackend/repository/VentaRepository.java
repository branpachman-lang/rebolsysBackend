package pe.com.rebolsysBackend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.rebolsysBackend.entity.Venta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    Optional<Venta> findByClaveRegistro(String clave);
    List<Venta> findAllByOrderByIdVentaDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Venta v where v.idVenta=:id")
    Optional<Venta> bloquear(@Param("id") Long id);
}
