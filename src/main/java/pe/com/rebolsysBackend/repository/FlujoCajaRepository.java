package pe.com.rebolsysBackend.repository;

import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.*;
import pe.com.rebolsysBackend.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface FlujoCajaRepository extends JpaRepository<FlujoCaja, Long>,
    JpaSpecificationExecutor<FlujoCaja>, FlujoCajaHistorialRepository {
  @EntityGraph(attributePaths = {"turnoCaja.usuario", "tipoMovimientoCaja"})
  List<FlujoCaja> findByTurnoCajaIdTurnoOrderByIdFlujoDesc(Long id);

  @Override
  @EntityGraph(attributePaths = {"turnoCaja.usuario", "tipoMovimientoCaja"})
  Page<FlujoCaja> findAll(Specification<FlujoCaja> filtro, Pageable pagina);

  Optional<FlujoCaja> findByClaveRegistro(String clave);

  @EntityGraph(attributePaths = "tipoMovimientoCaja")
  Page<FlujoCaja> findByTurnoCajaIdTurno(Long id, Pageable pagina);

  interface ImporteManual {
    Integer getSigno();
    BigDecimal getMonto();
  }

  @Query("""
      select f.tipoMovimientoCaja.signo as signo, sum(f.monto) as monto from FlujoCaja f
      where f.turnoCaja.idTurno = :id and f.pago is null group by f.tipoMovimientoCaja.signo
      """)
  List<ImporteManual> sumarManuales(Long id);
}
