package pe.com.rebolsysBackend.repository;

import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import pe.com.rebolsysBackend.entity.*;

public interface DetalleHabitacionRepository extends JpaRepository<DetalleHabitacion, Long> {
  List<DetalleHabitacion> findByItemIdItem(Long id);

  List<DetalleHabitacion> findByHabitacionIdHabitacion(Long id);
}
