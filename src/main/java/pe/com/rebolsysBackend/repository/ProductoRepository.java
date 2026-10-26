package pe.com.rebolsysBackend.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.rebolsysBackend.entity.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.idProducto=:id")
    Optional<Producto> bloquear(@Param("id") Long id);

    @Query(
        "select p from Producto p where lower(p.nombreProducto) like concat('%',:buscar,'%') and (:categoria is null or p.categoria.idCategoria=:categoria) and (:estado is null or p.estadoProducto=:estado)"
    )
    org.springframework.data.domain.Page<Producto> buscar(
        @Param("buscar") String buscar,
        @Param("categoria") Long categoria,
        @Param("estado") pe.com.rebolsysBackend.enums.EstadoProducto estado,
        org.springframework.data.domain.Pageable pageable
    );

    java.util.List<Producto> findByEstadoProductoOrderByNombreProductoAsc(
        pe.com.rebolsysBackend.enums.EstadoProducto estado
    );
}
