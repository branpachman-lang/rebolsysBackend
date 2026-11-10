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
import pe.com.rebolsysBackend.entity.TicketMantenimiento;
import pe.com.rebolsysBackend.enums.TipoAccionMantenimiento;

public interface TicketMantenimientoRepository extends JpaRepository<TicketMantenimiento, Long> {
  @EntityGraph(attributePaths = {"detalleInforme", "detalleInforme.detalleHabitacion",
      "detalleInforme.detalleHabitacion.item"})
  List<TicketMantenimiento> findByDetalleInformeInformeRevisionIdInformeRevisionInOrderByIdTicket(Collection<Long> ids);

  Optional<TicketMantenimiento> findByDetalleInformeIdDetalleInforme(Long idDetalleInforme);

  @Query("""
      select t.detalleInforme.informeRevision.idInformeRevision from TicketMantenimiento t where t.idTicket = :id
      """)
  Optional<Long> informeDe(Long id);

  @EntityGraph(attributePaths = {"detalleInforme", "detalleInforme.detalleHabitacion",
      "detalleInforme.detalleHabitacion.item", "detalleInforme.informeRevision",
      "detalleInforme.informeRevision.habitacion"})
  @Query("""
      select t from TicketMantenimiento t where
      lower(concat('TKT-', cast(t.idTicket as String))) like :nombre escape '!'
      and lower(t.detalleInforme.informeRevision.habitacion.numeroCuarto) like :numero escape '!'
      and (:accion is null or t.tipoAccion = :accion)
      and (:desde is null or t.fechaGeneracion >= :desde)
      and (:hasta is null or t.fechaGeneracion < :hasta)
      """)
  Page<TicketMantenimiento> buscar(String nombre, String numero, TipoAccionMantenimiento accion, LocalDateTime desde,
      LocalDateTime hasta, Pageable pagina);

  long countByEmpleadoIdEmpleadoAndFechaGeneracionBetween(Long idEmpleado, LocalDateTime desde, LocalDateTime hasta);
}
