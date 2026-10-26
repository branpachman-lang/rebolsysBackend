package pe.com.rebolsysBackend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.rebolsysBackend.entity.TipoComprobante;

public interface TipoComprobanteRepository extends JpaRepository<TipoComprobante, Long> {
    Optional<TipoComprobante> findByCodigo(String codigo);
}
