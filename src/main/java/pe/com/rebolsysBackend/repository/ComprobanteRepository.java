package pe.com.rebolsysBackend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.rebolsysBackend.entity.Comprobante;
import pe.com.rebolsysBackend.entity.DetalleReserva;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {
    @Query("select distinct d.comprobante from ComprobanteDetalle d where d.consumo.detalleReserva in :reservas")
    List<Comprobante> buscarPorReservas(@Param("reservas") List<DetalleReserva> reservas);

    @Query("select distinct d.comprobante from ComprobanteDetalle d where d.consumo.venta.idVenta=:id")
    List<Comprobante> buscarPorVenta(@Param("id") Long id);
}
