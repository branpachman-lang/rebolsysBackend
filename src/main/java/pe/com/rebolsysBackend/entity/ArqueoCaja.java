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
import org.hibernate.annotations.GeneratedColumn;

@Entity
@Table(
    name = "arqueo_caja",
    uniqueConstraints = {
        @UniqueConstraint(name = "arqueo_turno_forma_unq", columnNames = { "idTurno", "idFormaPago" }),
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArqueoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idArqueo", nullable = false, columnDefinition = "NUMBER")
    private Long idArqueo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idTurno",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "arqueo_turno_fk")
    )
    private TurnoCaja turnoCaja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idFormaPago",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "arqueo_pago_fk")
    )
    private FormaPago formaPago;

    @Column(name = "montoCalculado", nullable = false, precision = 10, scale = 2, columnDefinition = "NUMBER(10,2)")
    private BigDecimal montoCalculado;

    @Column(name = "montoDeclarado", nullable = false, precision = 10, scale = 2, columnDefinition = "NUMBER(10,2)")
    private BigDecimal montoDeclarado;

    @GeneratedColumn("montoDeclarado - montoCalculado")
    @Column(
        name = "diferencia",
        nullable = true,
        precision = 10,
        scale = 2,
        columnDefinition = "NUMBER(10,2)",
        insertable = false,
        updatable = false
    )
    private BigDecimal diferencia;
}
