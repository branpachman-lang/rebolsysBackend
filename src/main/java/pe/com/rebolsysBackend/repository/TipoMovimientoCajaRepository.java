package pe.com.rebolsysBackend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.rebolsysBackend.entity.TipoMovimientoCaja;

public interface TipoMovimientoCajaRepository extends JpaRepository<TipoMovimientoCaja, Long> {
    Optional<TipoMovimientoCaja> findByNombreMovimiento(String nombre);
}
