package pe.com.rebolsysBackend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.rebolsysBackend.entity.Huesped;

public interface HuespedRepository extends JpaRepository<Huesped, Long> {
    Optional<Huesped> findByPersonaNaturalIdPerfil(Long id);
}
