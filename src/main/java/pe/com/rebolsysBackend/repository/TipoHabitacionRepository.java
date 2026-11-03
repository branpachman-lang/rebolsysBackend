package pe.com.rebolsysBackend.repository;

import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.com.rebolsysBackend.entity.*;

public interface TipoHabitacionRepository extends JpaRepository<TipoHabitacion, Long> {
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from TipoHabitacion t where t.idTipoHabitacion=:id")
  Optional<TipoHabitacion> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
}
