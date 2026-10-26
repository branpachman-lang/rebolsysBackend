package pe.com.rebolsysBackend.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.rebolsysBackend.entity.DetalleReserva;

public interface DetalleReservaRepository extends JpaRepository<DetalleReserva, Long> {
    @Query(
        "select count(r) from DetalleReserva r where r.habitacion.idHabitacion = :habitacion and r.estadoDetalle in (pe.com.rebolsysBackend.enums.EstadoReserva.CONFIRMADA, pe.com.rebolsysBackend.enums.EstadoReserva.EN_CURSO) and r.fechaIngreso < :salida and r.fechaSalida > :ingreso and (:reserva is null or r.reserva.idReserva <> :reserva)"
    )
    long superpuestas(
        @Param("habitacion") Long habitacion,
        @Param("ingreso") LocalDateTime ingreso,
        @Param("salida") LocalDateTime salida,
        @Param("reserva") Long reserva
    );

    List<DetalleReserva> findByReservaIdReservaOrderByIdDetalleReserva(Long id);
    List<DetalleReserva> findAllByOrderByIdDetalleReservaDesc();

    @Query(
        "select r from DetalleReserva r join fetch r.reserva c join fetch c.cliente cl join fetch cl.perfil where r.estadoDetalle in (pe.com.rebolsysBackend.enums.EstadoReserva.CONFIRMADA, pe.com.rebolsysBackend.enums.EstadoReserva.EN_CURSO) and r.fechaIngreso < :salida and r.fechaSalida > :ingreso"
    )
    List<DetalleReserva> buscarEnPeriodo(
        @Param("ingreso") LocalDateTime ingreso,
        @Param("salida") LocalDateTime salida
    );
}
