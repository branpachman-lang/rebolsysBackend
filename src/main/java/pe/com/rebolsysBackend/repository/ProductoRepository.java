package pe.com.rebolsysBackend.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.rebolsysBackend.entity.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    boolean existsByCategoriaIdCategoria(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.idProducto=:id")
    Optional<Producto> bloquear(@Param("id") Long id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "categoria")
    @Query("""
        select p from Producto p where
        (lower(p.nombreProducto) like :buscar escape '!' or lower(p.categoria.nombreCategoria) like :buscar escape '!')
        and (:categoria is null or p.categoria.idCategoria=:categoria)
        and (:estado is null or p.estadoProducto=:estado)
        and (:stockBajo is null or (:stockBajo=true and p.stock<=p.stockMinimo)
          or (:stockBajo=false and p.stock>p.stockMinimo))
        """)
    org.springframework.data.domain.Page<Producto> buscar(
        @Param("buscar") String buscar,
        @Param("categoria") Long categoria,
        @Param("estado") pe.com.rebolsysBackend.enums.EstadoProducto estado,
        @Param("stockBajo") Boolean stockBajo,
        org.springframework.data.domain.Pageable pageable
    );

    java.util.List<Producto> findByEstadoProductoOrderByNombreProductoAsc(
        pe.com.rebolsysBackend.enums.EstadoProducto estado
    );
}
