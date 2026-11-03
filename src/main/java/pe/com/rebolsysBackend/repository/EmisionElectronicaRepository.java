package pe.com.rebolsysBackend.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.com.rebolsysBackend.entity.EmisionElectronica;

public interface EmisionElectronicaRepository extends JpaRepository<EmisionElectronica,Long> {
  Optional<EmisionElectronica> findByComprobanteIdComprobante(Long id);
  Optional<EmisionElectronica> findByClaveOperacion(String clave);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from EmisionElectronica e where e.comprobante.idComprobante=:id")
  Optional<EmisionElectronica> bloquearComprobante(@Param("id") Long id);
}
