package pe.com.rebolsysBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(
    name = "movimiento_inventario",
    uniqueConstraints = { @UniqueConstraint(name = "mi_consumo_unico_uq", columnNames = { "idConsumo" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idMovimiento", nullable = false, columnDefinition = "NUMBER")
    private Long idMovimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name = "idProducto",
        columnDefinition = "NUMBER",
        nullable = true,
        foreignKey = @ForeignKey(name = "mi_producto_fk")
    )
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name = "idItem",
        columnDefinition = "NUMBER",
        nullable = true,
        foreignKey = @ForeignKey(name = "mi_item_fk")
    )
    private Item item;

    @Column(name = "idConsumo", nullable = true, columnDefinition = "NUMBER")
    private Long idConsumo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name = "idDetalleHabitacion",
        columnDefinition = "NUMBER",
        nullable = true,
        foreignKey = @ForeignKey(name = "mi_detalle_hab_fk")
    )
    private DetalleHabitacion detalleHabitacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idTipoMovimientoAlmacen",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "mi_tipo_almacen_fk")
    )
    private TipoMovimientoAlmacen tipoMovimientoAlmacen;

    @Column(name = "cantidad", nullable = false, precision = 9, scale = 0, columnDefinition = "NUMBER(9,0)")
    private Integer cantidad;

    @Column(name = "stockResultante", nullable = false, precision = 9, scale = 0, columnDefinition = "NUMBER(9,0)")
    private Integer stockResultante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idUsuario",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "mi_usuario_fk")
    )
    private Usuario usuario;

    @Column(name = "observacion", nullable = true, length = 300, columnDefinition = "VARCHAR2(300)")
    private String observacion;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fechaMovimiento", nullable = false, columnDefinition = "TIMESTAMP", updatable = false)
    private LocalDateTime fechaMovimiento;

    // Vista de la FK compuesta; las columnas se escriben mediante los campos anteriores.
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumns(
        value = {
            @JoinColumn(
                name = "idConsumo",
                columnDefinition = "NUMBER",
                referencedColumnName = "idConsumo",
                insertable = false,
                updatable = false
            ),
            @JoinColumn(
                name = "idProducto",
                columnDefinition = "NUMBER",
                referencedColumnName = "idProducto",
                insertable = false,
                updatable = false
            ),
        },
        foreignKey = @ForeignKey(name = "mi_consumo_producto_fk")
    )
    private Consumo consumo;

    @PrePersist
    protected void completarFechaRegistro() {
        if (fechaMovimiento == null) fechaMovimiento = LocalDateTime.now();
    }
}
