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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "huesped",
    uniqueConstraints = { @UniqueConstraint(name = "huesped_persona_unq", columnNames = { "idPerfil" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Huesped {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idHuesped", nullable = false, columnDefinition = "NUMBER")
    private Long idHuesped;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idPerfil",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "huesped_persona_fk")
    )
    private PersonaNatural personaNatural;

    @Column(name = "restriccionesMedicas", nullable = true, length = 500, columnDefinition = "VARCHAR2(500)")
    private String restriccionesMedicas;
}
