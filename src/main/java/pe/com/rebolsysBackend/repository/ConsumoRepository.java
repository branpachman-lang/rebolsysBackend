package pe.com.rebolsysBackend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.rebolsysBackend.entity.Consumo;

public interface ConsumoRepository extends JpaRepository<Consumo, Long> {
    List<Consumo> findByDetalleReservaIdDetalleReserva(Long id);
    List<Consumo> findByVentaIdVentaOrderByIdConsumo(Long id);
}
