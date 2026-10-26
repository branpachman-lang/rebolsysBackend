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
    name = "tipo_movimiento_almacen",
    uniqueConstraints = {
        @UniqueConstraint(name = "tma_codigo_unq", columnNames = { "codigo" }),
        @UniqueConstraint(name = "tma_nombre_unq", columnNames = { "nombreMovimiento" }),
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoMovimientoAlmacen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTipoMovimientoAlmacen", nullable = false, columnDefinition = "NUMBER")
    private Long idTipoMovimientoAlmacen;

    @Column(name = "codigo", nullable = false, length = 10, columnDefinition = "VARCHAR2(10)")
    private String codigo;

    @Column(name = "nombreMovimiento", nullable = false, length = 50, columnDefinition = "VARCHAR2(50)")
    private String nombreMovimiento;

    @Column(name = "signo", nullable = false, precision = 2, scale = 0, columnDefinition = "NUMBER(2)")
    private Integer signo;
}
