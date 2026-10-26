package pe.com.rebolsysBackend.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.rebolsysBackend.entity.Reserva;
import pe.com.rebolsysBackend.enums.EstadoReserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    Optional<Reserva> findByClaveRegistro(String clave);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.idReserva = :id")
    Optional<Reserva> bloquear(@Param("id") Long id);

    @Query(
        """
        select r from Reserva r
        join r.cliente c join c.perfil p
        left join PersonaNatural n on n.idPerfil=p.idPerfil
        left join PersonaJuridica j on j.idPerfil=p.idPerfil
        where (:estado is null or r.estadoReserva=:estado)
        and (:texto is null
            or cast(r.idReserva as string) like concat('%',:texto,'%')
            or lower(p.numeroDocumento) like concat('%',:texto,'%')
            or lower(concat(n.nombres,' ',n.apellidos)) like concat('%',:texto,'%')
            or lower(j.razonSocial) like concat('%',:texto,'%')
            or exists(select d.idDetalleReserva from DetalleReserva d
                where d.reserva=r
                and (d.estadoDetalle<>pe.com.rebolsysBackend.enums.EstadoReserva.CANCELADA
                    or r.estadoReserva=pe.com.rebolsysBackend.enums.EstadoReserva.CANCELADA)
                and lower(d.habitacion.numeroCuarto) like concat('%',:texto,'%')))
        """
    )
    Page<Reserva> buscar(@Param("texto") String texto, @Param("estado") EstadoReserva estado, Pageable pageable);
}
