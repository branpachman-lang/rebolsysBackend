package pe.com.rebolsysBackend.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.rebolsysBackend.entity.SerieComprobante;
import pe.com.rebolsysBackend.entity.id.SerieComprobanteId;

public interface SerieComprobanteRepository
    extends JpaRepository<SerieComprobante, SerieComprobanteId> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from SerieComprobante s where s.id = :id")
  Optional<SerieComprobante> bloquear(@Param("id") SerieComprobanteId id);
}
