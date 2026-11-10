package pe.com.rebolsysBackend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.rebolsysBackend.entity.ArqueoCaja;

public interface ArqueoCajaRepository extends JpaRepository<ArqueoCaja, Long> {
  @EntityGraph(attributePaths = "formaPago")
  List<ArqueoCaja> findByTurnoCajaIdTurnoOrderByFormaPagoIdFormaPago(Long idTurno);
}
