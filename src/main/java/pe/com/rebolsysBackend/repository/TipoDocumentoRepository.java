package pe.com.rebolsysBackend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.rebolsysBackend.entity.TipoDocumento;

public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Long> {
    List<TipoDocumento> findByNombreDocumentoNotOrderByIdTipoDocumento(String nombre);
}
