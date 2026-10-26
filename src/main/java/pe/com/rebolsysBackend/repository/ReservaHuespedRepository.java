package pe.com.rebolsysBackend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.rebolsysBackend.entity.ReservaHuesped;
import pe.com.rebolsysBackend.entity.id.ReservaHuespedId;

public interface ReservaHuespedRepository extends JpaRepository<ReservaHuesped, ReservaHuespedId> {
    List<ReservaHuesped> findByDetalleReservaIdDetalleReserva(Long id);

    @Modifying
    @Query("delete from ReservaHuesped rh where rh.detalleReserva.idDetalleReserva=:id")
    void deleteByDetalleReservaIdDetalleReserva(@Param("id") Long id);
}
