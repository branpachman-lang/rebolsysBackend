package pe.com.rebolsysBackend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.rebolsysBackend.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
  Optional<Cliente> findByPerfilIdPerfil(Long id);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select c from Cliente c where c.idCliente=:id")
  java.util.Optional<Cliente> bloquear(
      @org.springframework.data.repository.query.Param("id") Long id);
}
