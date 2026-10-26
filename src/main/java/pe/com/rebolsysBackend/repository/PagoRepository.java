package pe.com.rebolsysBackend.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.rebolsysBackend.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByComprobanteIdComprobante(Long id);
    Optional<Pago> findByClaveIdempotencia(String clave);
    List<Pago> findByClaveIdempotenciaIn(java.util.Collection<String> claves);
}
