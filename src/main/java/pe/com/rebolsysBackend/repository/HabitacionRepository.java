package pe.com.rebolsysBackend.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.rebolsysBackend.entity.Habitacion;

public interface HabitacionRepository extends JpaRepository<Habitacion, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Habitacion h where h.idHabitacion = :id")
    Optional<Habitacion> bloquear(@Param("id") Long id);
}
