package pe.com.rebolsysBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "empleado",
    uniqueConstraints = { @UniqueConstraint(name = "empleado_persona_unq", columnNames = { "idPerfil" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idEmpleado", nullable = false, columnDefinition = "NUMBER")
    private Long idEmpleado;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idPerfil",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "empleado_persona_fk")
    )
    private PersonaNatural personaNatural;

    @Column(name = "fechaContratacion", nullable = false, columnDefinition = "DATE")
    private LocalDate fechaContratacion;

    @Column(name = "fechaCese", nullable = true, columnDefinition = "DATE")
    private LocalDate fechaCese;
}
