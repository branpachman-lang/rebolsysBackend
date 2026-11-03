package pe.com.rebolsysBackend.repository;

import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.com.rebolsysBackend.entity.*;

public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Long> {
  List<TurnoCaja> findByUsuarioIdUsuarioAndEstadoSesionAndFechaCierreIsNullOrderByIdTurno(
      Long usuario, Integer estado);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from TurnoCaja t where t.idTurno=:id")
  Optional<TurnoCaja> bloquear(@Param("id") Long id);
}
