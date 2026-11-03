package pe.com.rebolsysBackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.rebolsysBackend.entity.ListaNegra;

public interface ListaNegraRepository extends JpaRepository<ListaNegra, Long> {
  boolean existsByPerfilIdPerfil(Long id);
}
