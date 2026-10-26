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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoHabitacion;

@Entity
@Table(
    name = "habitacion",
    uniqueConstraints = { @UniqueConstraint(name = "habitacion_numero_unq", columnNames = { "numeroCuarto" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Habitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idHabitacion", nullable = false, columnDefinition = "NUMBER")
    private Long idHabitacion;

    @Column(name = "numeroCuarto", nullable = false, length = 10, columnDefinition = "VARCHAR2(10)")
    private String numeroCuarto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idTipo",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "habitacion_tipo_fk")
    )
    private TipoHabitacion tipoHabitacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estadoHabitacion", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private EstadoHabitacion estadoHabitacion;

    @Column(name = "capacidadPersonas", nullable = false, precision = 2, scale = 0, columnDefinition = "NUMBER(2)")
    private Integer capacidadPersonas;

    @Column(name = "plazasCama", nullable = false, precision = 2, scale = 0, columnDefinition = "NUMBER(2)")
    private Integer plazasCama;

    @Column(name = "maxMenoresSinCama", nullable = false, precision = 2, scale = 0, columnDefinition = "NUMBER(2)")
    private Integer maxMenoresSinCama = 0;
}
