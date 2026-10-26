package pe.com.rebolsysBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import pe.com.rebolsysBackend.enums.EstadoCargo;
import pe.com.rebolsysBackend.enums.EstadoCobro;
import pe.com.rebolsysBackend.enums.TipoConsumo;

@Entity
@Table(
    name = "consumo",
    uniqueConstraints = { @UniqueConstraint(name = "consumo_producto_uq", columnNames = { "idConsumo", "idProducto" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Consumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idConsumo", nullable = false, columnDefinition = "NUMBER")
    private Long idConsumo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name = "idCliente",
        columnDefinition = "NUMBER",
        nullable = true,
        foreignKey = @ForeignKey(name = "consumo_cliente_fk")
    )
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name = "idDetalleReserva",
        columnDefinition = "NUMBER",
        nullable = true,
        foreignKey = @ForeignKey(name = "consumo_detalle_reserva_fk")
    )
    private DetalleReserva detalleReserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name = "idProducto",
        columnDefinition = "NUMBER",
        nullable = true,
        foreignKey = @ForeignKey(name = "consumo_producto_fk")
    )
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idUsuario",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "consumo_usuario_fk")
    )
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipoConsumo", nullable = false, length = 20, columnDefinition = "VARCHAR2(20)")
    private TipoConsumo tipoConsumo;

    @Column(name = "descripcion", nullable = false, length = 300, columnDefinition = "VARCHAR2(300)")
    private String descripcion;

    @ColumnDefault("1")
    @Column(name = "cantidad", nullable = false, precision = 12, scale = 3, columnDefinition = "NUMBER(12,3)")
    private BigDecimal cantidad = new BigDecimal("1");

    @Column(name = "valorUnitario", nullable = false, precision = 12, scale = 4, columnDefinition = "NUMBER(12,4)")
    private BigDecimal valorUnitario;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2, columnDefinition = "NUMBER(12,2)")
    private BigDecimal subtotal;

    @Column(name = "igv", nullable = false, precision = 12, scale = 2, columnDefinition = "NUMBER(12,2)")
    private BigDecimal igv;

    @Column(name = "total", nullable = false, precision = 12, scale = 2, columnDefinition = "NUMBER(12,2)")
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "estadoCargo", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private EstadoCargo estadoCargo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estadoCobro", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private EstadoCobro estadoCobro;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fechaConsumo", nullable = false, columnDefinition = "TIMESTAMP", updatable = false)
    private LocalDateTime fechaConsumo;

    @PrePersist
    protected void completarFechaRegistro() {
        if (fechaConsumo == null) fechaConsumo = LocalDateTime.now();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idVenta", columnDefinition = "NUMBER", foreignKey = @ForeignKey(name = "consumo_venta_fk"))
    private Venta venta;
}
