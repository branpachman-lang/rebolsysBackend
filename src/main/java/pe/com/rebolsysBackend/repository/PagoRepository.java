package pe.com.rebolsysBackend.repository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.com.rebolsysBackend.entity.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByComprobanteIdComprobante(Long id);
    Optional<Pago> findByClaveIdempotencia(String clave);
    List<Pago> findByClaveIdempotenciaIn(java.util.Collection<String> claves);

    interface ImporteForma {
        Long getIdFormaPago();
        BigDecimal getMonto();
    }

    @Query("""
        select p.formaPago.idFormaPago as idFormaPago, sum(p.monto) as monto from Pago p
        where p.turnoCaja.idTurno = :id and p.verificado = 1 group by p.formaPago.idFormaPago
        """)
    List<ImporteForma> sumarVerificados(Long id);
}
