package pe.com.rebolsysBackend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.rebolsysBackend.entity.TurnoCaja;
import pe.com.rebolsysBackend.entity.Usuario;

public interface TurnoCajaRepository extends JpaRepository<TurnoCaja, Long> {
    List<TurnoCaja> findByUsuarioAndFechaCierreIsNullOrderByIdTurno(Usuario usuario);
}
