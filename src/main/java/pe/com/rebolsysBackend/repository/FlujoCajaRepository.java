package pe.com.rebolsysBackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.rebolsysBackend.entity.FlujoCaja;

public interface FlujoCajaRepository extends JpaRepository<FlujoCaja, Long> {
    java.util.List<FlujoCaja> findByPagoIdPagoAndTipoMovimientoCajaSigno(Long idPago, Integer signo);
}
