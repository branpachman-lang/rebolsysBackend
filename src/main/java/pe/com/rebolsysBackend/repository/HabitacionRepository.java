package pe.com.rebolsysBackend.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.rebolsysBackend.entity.Habitacion;

public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {
    boolean existsByNumeroCuarto(String numero);
    boolean existsByNumeroCuartoAndIdHabitacionNot(String numero, Long id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "tipoHabitacion")
    @Query("""
        select h from Habitacion h where
        (lower(h.numeroCuarto) like :buscar escape '!' or lower(h.tipoHabitacion.nombreTipo) like :buscar escape '!')
        and (:tipo is null or h.tipoHabitacion.idTipoHabitacion=:tipo)
        and (:estado is null or h.estadoHabitacion=:estado)
        and (:capacidad is null or h.capacidadPersonas>=:capacidad)
        """)
    org.springframework.data.domain.Page<Habitacion> buscarConfiguracion(
        @Param("buscar") String buscar, @Param("tipo") Long tipo,
        @Param("estado") pe.com.rebolsysBackend.enums.EstadoHabitacion estado,
        @Param("capacidad") Integer capacidad, org.springframework.data.domain.Pageable pagina);

    boolean existsByTipoHabitacionIdTipoHabitacion(Long id);

    boolean existsByTipoHabitacionIdTipoHabitacionAndCapacidadPersonasGreaterThan(Long id, Integer capacidad);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Habitacion h where h.idHabitacion = :id")
    Optional<Habitacion> bloquear(@Param("id") Long id);
}
