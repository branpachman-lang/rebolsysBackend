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
@Table(name = "turno_caja")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TurnoCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTurno", nullable = false, columnDefinition = "NUMBER")
    private Long idTurno;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idUsuario",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "turno_usuario_fk")
    )
    private Usuario usuario;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fechaApertura", nullable = false, columnDefinition = "TIMESTAMP", updatable = false)
    private LocalDateTime fechaApertura;

    @Column(name = "fechaCierre", nullable = true, columnDefinition = "TIMESTAMP")
    private LocalDateTime fechaCierre;

    @ColumnDefault("0.00")
    @Column(name = "montoBase", nullable = false, precision = 10, scale = 2, columnDefinition = "NUMBER(10,2)")
    private BigDecimal montoBase = new BigDecimal("0.00");

    @ColumnDefault("0")
    @Column(name = "estadoSesion", nullable = false, precision = 1, scale = 0, columnDefinition = "NUMBER(1)")
    private Integer estadoSesion = 0;

    @PrePersist
    protected void completarFechaRegistro() {
        if (fechaApertura == null) fechaApertura = LocalDateTime.now();
    }
}
