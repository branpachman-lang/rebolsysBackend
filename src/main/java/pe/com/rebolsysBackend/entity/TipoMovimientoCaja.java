package pe.com.rebolsysBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "tipo_movimiento_caja",
    uniqueConstraints = { @UniqueConstraint(name = "tipo_movimiento_caja_unq", columnNames = { "nombreMovimiento" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoMovimientoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTipoMovimiento", nullable = false, columnDefinition = "NUMBER")
    private Long idTipoMovimiento;

    @Column(name = "nombreMovimiento", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private String nombreMovimiento;

    @Column(name = "SIGNO", nullable = false, precision = 2, scale = 0, columnDefinition = "NUMBER(2)")
    private Integer signo;
}
