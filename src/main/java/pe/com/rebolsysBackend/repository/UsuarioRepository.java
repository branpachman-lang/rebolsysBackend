package pe.com.rebolsysBackend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.rebolsysBackend.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.idUsuario=:id")
  Optional<Usuario> bloquear(@org.springframework.data.repository.query.Param("id") Long id);

  Optional<Usuario> findByUsername(String username);

  Optional<Usuario> findByUsernameIgnoreCase(String username);

  boolean existsByUsernameIgnoreCase(String username);
  boolean existsByUsernameIgnoreCaseAndIdUsuarioNot(String username, Long idUsuario);

  Optional<Usuario> findByEmpleadoIdEmpleado(Long idEmpleado);

  boolean existsByEmpleadoIdEmpleado(Long idEmpleado);
}
