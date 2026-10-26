package pe.com.rebolsysBackend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.rebolsysBackend.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findAllByOrderByNombreCategoriaAsc();
    boolean existsByNombreCategoria(String nombre);
}
