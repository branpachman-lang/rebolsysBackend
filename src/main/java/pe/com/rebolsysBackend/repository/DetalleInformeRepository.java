package pe.com.rebolsysBackend.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.rebolsysBackend.entity.DetalleInforme;

public interface DetalleInformeRepository extends JpaRepository<DetalleInforme, Long> {
  @EntityGraph(attributePaths = {"item", "habitacion"})
  List<DetalleInforme> findByInformeRevisionIdInformeRevisionInOrderByIdDetalleInforme(Collection<Long> ids);
}
