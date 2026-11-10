package pe.com.rebolsysBackend.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.com.rebolsysBackend.entity.InformeRevision;
import pe.com.rebolsysBackend.enums.EstadoInforme;

public interface InformeRevisionRepository extends JpaRepository<InformeRevision, Long> {
  boolean existsByHabitacionIdHabitacionAndEstadoInformeIn(Long idHabitacion, Collection<EstadoInforme> estados);

  boolean existsByHabitacionIdHabitacion(Long idHabitacion);

  @EntityGraph(attributePaths = {"habitacion", "usuario"})
  List<InformeRevision> findByEstadoInformeIn(Collection<EstadoInforme> estados);

  @EntityGraph(attributePaths = {"habitacion", "usuario"})
  Optional<InformeRevision> findByHabitacionIdHabitacionAndEstadoInformeIn(
      Long idHabitacion, Collection<EstadoInforme> estados);

  Optional<InformeRevision> findFirstByHabitacionIdHabitacionAndEstadoInformeNotOrderByFechaHoraDescIdInformeRevisionDesc(
      Long idHabitacion, EstadoInforme estado);

  @Query("select i.habitacion.idHabitacion from InformeRevision i where i.idInformeRevision = :id")
  Optional<Long> habitacionDe(Long id);

  @EntityGraph(attributePaths = {"habitacion", "usuario"})
  @Query("""
      select i from InformeRevision i where i.estadoInforme <> pe.com.rebolsysBackend.enums.EstadoInforme.BORRADOR
      and (:estado is null or i.estadoInforme = :estado)
      and lower(concat('INF-', cast(i.idInformeRevision as String))) like :nombre escape '!'
      and lower(i.habitacion.numeroCuarto) like :numero escape '!'
      and (:desde is null or i.fechaHora >= :desde)
      and (:hasta is null or i.fechaHora < :hasta)
      """)
  Page<InformeRevision> buscar(String nombre, String numero, EstadoInforme estado, LocalDateTime desde,
      LocalDateTime hasta, Pageable pagina);

  long countByUsuarioIdUsuarioAndEstadoInformeAndFechaHoraBetween(
      Long idUsuario, EstadoInforme estado, LocalDateTime desde, LocalDateTime hasta);

  long countByEstadoInforme(EstadoInforme estado);
}
