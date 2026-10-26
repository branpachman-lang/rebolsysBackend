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

@Entity
@Table(
    name = "pago",
    uniqueConstraints = { @UniqueConstraint(name = "pago_idem_unq", columnNames = { "claveIdempotencia" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPago", nullable = false, columnDefinition = "NUMBER")
    private Long idPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idComprobante",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "pago_comprobante_fk")
    )
    private Comprobante comprobante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idFormaPago",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "pago_formapago_fk")
    )
    private FormaPago formaPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idUsuario",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "pago_usuario_fk")
    )
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idTurno",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "pago_turno_fk")
    )
    private TurnoCaja turnoCaja;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2, columnDefinition = "NUMBER(10,2)")
    private BigDecimal monto;

    @Column(name = "numeroOperacion", nullable = true, length = 50, columnDefinition = "VARCHAR2(50)")
    private String numeroOperacion;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fechaPago", nullable = false, columnDefinition = "TIMESTAMP", updatable = false)
    private LocalDateTime fechaPago;

    @ColumnDefault("0")
    @Column(name = "verificado", nullable = false, precision = 1, scale = 0, columnDefinition = "NUMBER(1)")
    private Integer verificado = 0;

    @Column(name = "claveIdempotencia", nullable = true, length = 64, columnDefinition = "VARCHAR2(64)")
    private String claveIdempotencia;

    @PrePersist
    protected void completarFechaRegistro() {
        if (fechaPago == null) fechaPago = LocalDateTime.now();
    }
}
