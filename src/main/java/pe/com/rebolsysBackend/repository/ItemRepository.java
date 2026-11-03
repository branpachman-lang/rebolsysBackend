package pe.com.rebolsysBackend.repository;

import jakarta.persistence.LockModeType;
import java.time.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.com.rebolsysBackend.entity.*;

public interface ItemRepository extends JpaRepository<Item, Long> {
  boolean existsByTipoItemIdTipoItem(Long id);

  @EntityGraph(attributePaths = "tipoItem")
  @Query("""
      select i from Item i where
      (lower(i.nombreItem) like :buscar escape '!' or lower(i.tipoItem.nombreItem) like :buscar escape '!')
      and (:tipo is null or i.tipoItem.idTipoItem=:tipo)
      and (:estado is null or i.estadoItem=:estado)
      and (:sinAsignar is null
        or (:sinAsignar=true and i.estadoItem<>pe.com.rebolsysBackend.enums.EstadoItem.EN_HABITACION)
        or (:sinAsignar=false and i.estadoItem=pe.com.rebolsysBackend.enums.EstadoItem.EN_HABITACION))
      and (:habitacion is null or (i.estadoItem=pe.com.rebolsysBackend.enums.EstadoItem.EN_HABITACION
        and exists (select m.idMovimiento from MovimientoInventario m where m.item=i
          and m.idMovimiento=(select max(x.idMovimiento) from MovimientoInventario x where x.item=i)
          and m.detalleHabitacion.habitacion.idHabitacion=:habitacion)))
      and (:conservacion is null or exists (select m.idMovimiento from MovimientoInventario m where m.item=i
        and m.idMovimiento=(select max(x.idMovimiento) from MovimientoInventario x where x.item=i)
        and m.detalleHabitacion.estadoActualItem=:conservacion))
      """)
  org.springframework.data.domain.Page<Item> buscarConfiguracion(
      @Param("buscar") String buscar, @Param("tipo") Long tipo,
      @Param("estado") pe.com.rebolsysBackend.enums.EstadoItem estado,
      @Param("habitacion") Long habitacion, @Param("sinAsignar") Boolean sinAsignar,
      @Param("conservacion") pe.com.rebolsysBackend.enums.EstadoConservacion conservacion,
      org.springframework.data.domain.Pageable pagina);

  @EntityGraph(attributePaths = "tipoItem")
  @Query("""
      select i from Item i where i.estadoItem=pe.com.rebolsysBackend.enums.EstadoItem.EN_HABITACION
      and exists (select m.idMovimiento from MovimientoInventario m where m.item=i
        and m.idMovimiento=(select max(x.idMovimiento) from MovimientoInventario x where x.item=i)
        and m.detalleHabitacion.habitacion.idHabitacion=:habitacion)
      order by i.nombreItem, i.idItem
      """)
  List<Item> actualesHabitacion(@Param("habitacion") Long habitacion);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select i from Item i where i.idItem=:id")
  Optional<Item> bloquear(@Param("id") Long id);
}
