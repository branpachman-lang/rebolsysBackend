package pe.com.rebolsysBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "comprobante_detalle",
    uniqueConstraints = { @UniqueConstraint(name = "cd_linea_unq", columnNames = { "idComprobante", "numeroLinea" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComprobanteDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idComprobanteDetalle", nullable = false, columnDefinition = "NUMBER")
    private Long idComprobanteDetalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idComprobante",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "cd_comprobante_fk")
    )
    private Comprobante comprobante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idConsumo",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "cd_consumo_fk")
    )
    private Consumo consumo;

    @Column(name = "numeroLinea", nullable = false, precision = 4, scale = 0, columnDefinition = "NUMBER(4)")
    private Integer numeroLinea;

    @Column(name = "descripcion", nullable = false, length = 300, columnDefinition = "VARCHAR2(300)")
    private String descripcion;

    @Column(name = "cantidad", nullable = false, precision = 12, scale = 3, columnDefinition = "NUMBER(12,3)")
    private BigDecimal cantidad;

    @Column(name = "valorUnitario", nullable = false, precision = 12, scale = 4, columnDefinition = "NUMBER(12,4)")
    private BigDecimal valorUnitario;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2, columnDefinition = "NUMBER(12,2)")
    private BigDecimal subtotal;

    @Column(name = "igv", nullable = false, precision = 12, scale = 2, columnDefinition = "NUMBER(12,2)")
    private BigDecimal igv;

    @Column(name = "total", nullable = false, precision = 12, scale = 2, columnDefinition = "NUMBER(12,2)")
    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name = "IDHABITACION",
        columnDefinition = "NUMBER",
        nullable = true,
        foreignKey = @ForeignKey(name = "cd_habitacion_fk")
    )
    private Habitacion habitacion;
}
