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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoInforme;

@Entity
@Table(name = "informe_revision")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InformeRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idInformeRevision", nullable = false, columnDefinition = "NUMBER")
    private Long idInformeRevision;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name = "idDetalleReserva",
        columnDefinition = "NUMBER",
        nullable = true,
        foreignKey = @ForeignKey(name = "informe_detalle_reserva_fk")
    )
    private DetalleReserva detalleReserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idHabitacion",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "informe_habitacion_fk")
    )
    private Habitacion habitacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idUsuario",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "informe_usuario_fk")
    )
    private Usuario usuario;

    @Column(name = "fechaHora", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime fechaHora;

    @Column(name = "observaciones", nullable = false, length = 1000, columnDefinition = "VARCHAR2(1000)")
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(name = "estadoInforme", nullable = false, length = 20, columnDefinition = "VARCHAR2(20)")
    private EstadoInforme estadoInforme = EstadoInforme.APROBADO;
}
