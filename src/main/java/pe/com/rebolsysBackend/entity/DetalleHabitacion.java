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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoConservacion;

@Entity
@Table(name = "detalle_habitacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleHabitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDetalleHabitacion", nullable = false, columnDefinition = "NUMBER")
    private Long idDetalleHabitacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idHabitacion",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "dh_habitacion_fk")
    )
    private Habitacion habitacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idItem",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "dh_item_fk")
    )
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(name = "estadoActualItem", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private EstadoConservacion estadoActualItem;
}
