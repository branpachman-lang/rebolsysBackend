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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "permiso",
    uniqueConstraints = { @UniqueConstraint(name = "permiso_mod_acc_unq", columnNames = { "idModulo", "accion" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPermiso", nullable = false, columnDefinition = "NUMBER")
    private Long idPermiso;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idModulo",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "permiso_modulo_fk")
    )
    private Modulo modulo;

    @Column(name = "accion", nullable = false, length = 100, columnDefinition = "VARCHAR2(100)")
    private String accion;
}
