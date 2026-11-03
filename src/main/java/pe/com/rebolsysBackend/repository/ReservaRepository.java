package pe.com.rebolsysBackend.repository;

import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.com.rebolsysBackend.entity.*;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
  Optional<Reserva> findByClaveRegistro(String clave);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Reserva r where r.idReserva=:id")
  Optional<Reserva> bloquear(@Param("id") Long id);
}
