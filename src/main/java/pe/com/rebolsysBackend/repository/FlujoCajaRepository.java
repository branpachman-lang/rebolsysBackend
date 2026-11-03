package pe.com.rebolsysBackend.repository;

import java.time.*;
import java.util.*;
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
}
