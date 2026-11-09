package pe.com.rebolsysBackend.repository;

import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.com.rebolsysBackend.entity.*;

public interface DetalleHabitacionRepository extends JpaRepository<DetalleHabitacion, Long> {
  List<DetalleHabitacion> findByItemIdItem(Long id);

  List<DetalleHabitacion> findByHabitacionIdHabitacion(Long id);

  @Query("""
      select d from MovimientoInventario m join m.detalleHabitacion d join fetch d.item i
      where d.habitacion.idHabitacion = :id and i.estadoItem = pe.com.rebolsysBackend.enums.EstadoItem.EN_HABITACION
      and m.idMovimiento = (select max(x.idMovimiento) from MovimientoInventario x where x.item = i)
      order by i.nombreItem, i.idItem
      """)
  List<DetalleHabitacion> actuales(@Param("id") Long idHabitacion);
}
