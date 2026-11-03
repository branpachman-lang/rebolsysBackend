package pe.com.rebolsysBackend.repository;

import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.com.rebolsysBackend.entity.*;

public interface DetalleReservaRepository extends JpaRepository<DetalleReserva, Long> {
  List<DetalleReserva> findByReservaIdReservaOrderByIdDetalleReserva(Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select d from DetalleReserva d where d.idDetalleReserva=:id")
  Optional<DetalleReserva> bloquear(@Param("id") Long id);

  @Query(
      "select count(d) from DetalleReserva d where d.habitacion.idHabitacion=:habitacion and"
          + " ((d.estadoDetalle=pe.com.rebolsysBackend.enums.EstadoReserva.CONFIRMADA and"
          + " d.fechaIngreso<:salida and d.fechaSalida>:ingreso) or"
          + " (d.estadoDetalle=pe.com.rebolsysBackend.enums.EstadoReserva.EN_CURSO and"
          + " d.fechaIngreso<:salida)) and (:excluir is null or d.idDetalleReserva<>:excluir)")
  long superpuestas(
      @Param("habitacion") Long habitacion,
      @Param("ingreso") LocalDateTime ingreso,
      @Param("salida") LocalDateTime salida,
      @Param("excluir") Long excluir);

  @Query(
      "select d from DetalleReserva d join fetch d.reserva r join fetch r.cliente c join fetch"
          + " c.perfil where (d.estadoDetalle=pe.com.rebolsysBackend.enums.EstadoReserva.CONFIRMADA"
          + " and d.fechaIngreso<:salida and d.fechaSalida>:ingreso) or"
          + " (d.estadoDetalle=pe.com.rebolsysBackend.enums.EstadoReserva.EN_CURSO and"
          + " d.fechaIngreso<:salida)")
  List<DetalleReserva> buscarEnPeriodo(
      @Param("ingreso") LocalDateTime ingreso, @Param("salida") LocalDateTime salida);

  List<DetalleReserva> findByHabitacionIdHabitacion(Long id);
}
