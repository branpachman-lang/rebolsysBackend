package pe.com.rebolsysBackend.repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import pe.com.rebolsysBackend.entity.Perfil;

public interface PerfilRepository extends JpaRepository<Perfil, Long> {
    Optional<Perfil> findByTipoDocumentoIdTipoDocumentoAndNumeroDocumento(Long tipo, String numero);

    Optional<Perfil> findByIdPerfilAndEsCliente(Long idPerfil, Integer esCliente);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Perfil p where p.idPerfil=:id and p.esCliente=1")
    Optional<Perfil> bloquearCliente(Long id);
}
