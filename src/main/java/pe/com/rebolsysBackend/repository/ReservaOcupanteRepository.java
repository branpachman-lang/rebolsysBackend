package pe.com.rebolsysBackend.repository;

import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.com.rebolsysBackend.entity.*;

public interface ReservaOcupanteRepository extends JpaRepository<ReservaOcupante, Long> {
  List<ReservaOcupante> findByDetalleReservaIdDetalleReservaOrderByIdReservaOcupante(Long id);

  @Query(
      "select o from ReservaOcupante o where o.detalleReserva.habitacion.idHabitacion=:id and"
          + " o.detalleReserva.estadoDetalle in"
          + " (pe.com.rebolsysBackend.enums.EstadoReserva.CONFIRMADA,"
          + " pe.com.rebolsysBackend.enums.EstadoReserva.EN_CURSO)")
  List<ReservaOcupante> activosHabitacion(@Param("id") Long id);

  @Query(
      "select count(o) from ReservaOcupante o where o.huesped.perfil.idPerfil=:cliente and"
          + " ((o.detalleReserva.estadoDetalle=pe.com.rebolsysBackend.enums.EstadoReserva.CONFIRMADA"
          + " and o.detalleReserva.fechaIngreso<:salida and o.detalleReserva.fechaSalida>:ingreso)"
          + " or (o.detalleReserva.estadoDetalle=pe.com.rebolsysBackend.enums.EstadoReserva.EN_CURSO"
          + " and o.detalleReserva.fechaIngreso<:salida)) and (:excluir is null or"
          + " o.detalleReserva.idDetalleReserva<>:excluir)")
  long superposicionesCliente(
      @Param("cliente") Long cliente,
      @Param("ingreso") LocalDateTime ingreso,
      @Param("salida") LocalDateTime salida,
      @Param("excluir") Long excluir);

  default long superposicionesCliente(Long cliente, LocalDateTime ingreso, LocalDateTime salida) {
    return superposicionesCliente(cliente, ingreso, salida, null);
  }

  @Modifying
  @Query("delete from ReservaOcupante o where o.detalleReserva.idDetalleReserva=:id")
  void eliminarDeDetalle(@Param("id") Long id);
}
