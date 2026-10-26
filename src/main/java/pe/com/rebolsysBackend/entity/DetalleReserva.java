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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import pe.com.rebolsysBackend.enums.EstadoReserva;

@Entity
@Table(name = "detalle_reserva")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleReserva {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idReserva",
        nullable = false,
        columnDefinition = "NUMBER",
        foreignKey = @ForeignKey(name = "detalle_reserva_cabecera_fk")
    )
    private Reserva reserva;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDetalleReserva", nullable = false, columnDefinition = "NUMBER")
    private Long idDetalleReserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idHabitacion",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "detalle_reserva_habitacion_fk")
    )
    private Habitacion habitacion;

    @ColumnDefault("0")
    @Column(name = "costoEstadia", nullable = false, precision = 10, scale = 2, columnDefinition = "NUMBER(10,2)")
    private BigDecimal costoEstadia = new BigDecimal("0");

    @Column(name = "fechaIngreso", nullable = false, columnDefinition = "DATE")
    private LocalDateTime fechaIngreso;

    @Column(name = "fechaSalida", nullable = false, columnDefinition = "DATE")
    private LocalDateTime fechaSalida;

    @Enumerated(EnumType.STRING)
    @Column(name = "estadoDetalle", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private EstadoReserva estadoDetalle;
}
