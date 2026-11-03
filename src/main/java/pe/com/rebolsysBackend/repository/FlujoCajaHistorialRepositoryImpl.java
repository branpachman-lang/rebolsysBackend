package pe.com.rebolsysBackend.repository;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import pe.com.rebolsysBackend.dto.GastoResumenDTO;
import pe.com.rebolsysBackend.entity.FlujoCaja;

@RequiredArgsConstructor
public class FlujoCajaHistorialRepositoryImpl implements FlujoCajaHistorialRepository {
  private final EntityManager em;

  @Override
  public GastoResumenDTO resumir(Specification<FlujoCaja> filtro) {
    var cb = em.getCriteriaBuilder();
    var query = cb.createQuery(Object[].class);
    var root = query.from(FlujoCaja.class);
    query.select(cb.array(cb.count(root), cb.coalesce(cb.sum(root.<BigDecimal>get("monto")), BigDecimal.ZERO)));
    query.where(filtro.toPredicate(root, query, cb));
    var valores = em.createQuery(query).getSingleResult();
    return new GastoResumenDTO(((Number) valores[0]).longValue(), (BigDecimal) valores[1]);
  }
}
