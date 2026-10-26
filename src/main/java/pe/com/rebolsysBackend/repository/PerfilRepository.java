package pe.com.rebolsysBackend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.rebolsysBackend.entity.Perfil;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {
    Optional<Perfil> findByTipoDocumentoIdTipoDocumentoAndNumeroDocumento(Long tipo, String numero);
}
