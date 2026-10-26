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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "flujo_caja")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlujoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idFlujo", nullable = false, columnDefinition = "NUMBER")
    private Long idFlujo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idTurno",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "flujo_turno_fk")
    )
    private TurnoCaja turnoCaja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idTipoMovimiento",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "flujo_tipo_fk")
    )
    private TipoMovimientoCaja tipoMovimientoCaja;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name = "idPago",
        columnDefinition = "NUMBER",
        nullable = true,
        foreignKey = @ForeignKey(name = "flujo_pago_fk")
    )
    private Pago pago;

    @Column(name = "concepto", nullable = false, length = 1000, columnDefinition = "VARCHAR2(1000)")
    private String concepto;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2, columnDefinition = "NUMBER(10,2)")
    private BigDecimal monto;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fechaHora", nullable = false, columnDefinition = "TIMESTAMP", updatable = false)
    private LocalDateTime fechaHora;

    @Column(name = "documentoRespaldo", nullable = true, length = 100, columnDefinition = "VARCHAR2(100)")
    private String documentoRespaldo;

    @Column(name = "evidenciaRuta", nullable = true, length = 300, columnDefinition = "VARCHAR2(300)")
    private String evidenciaRuta;

    @Column(name = "evidenciaSha256", nullable = true, length = 64, columnDefinition = "CHAR(64)")
    private String evidenciaSha256;

    @PrePersist
    protected void completarFechaRegistro() {
        if (fechaHora == null) fechaHora = LocalDateTime.now();
    }
}
