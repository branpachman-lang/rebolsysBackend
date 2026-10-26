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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoUsuario;

@Entity
@Table(
    name = "usuario",
    uniqueConstraints = {
        @UniqueConstraint(name = "usuario_empleado_unq", columnNames = { "idEmpleado" }),
        @UniqueConstraint(name = "usuario_username_unq", columnNames = { "username" }),
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idUsuario", nullable = false, columnDefinition = "NUMBER")
    private Long idUsuario;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idEmpleado",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "usuario_empleado_fk")
    )
    private Empleado empleado;

    @Column(name = "username", nullable = false, length = 50, columnDefinition = "VARCHAR2(50)")
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "estadoUsuario", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private EstadoUsuario estadoUsuario;
}
