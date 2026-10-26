package pe.com.rebolsysBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "persona_natural")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonaNatural {

    @Id
    @Column(name = "idPerfil", nullable = false, columnDefinition = "NUMBER")
    private Long idPerfil;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idPerfil",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "persona_perfil_fk")
    )
    private Perfil perfil;

    @Column(name = "nombres", nullable = false, length = 100, columnDefinition = "VARCHAR2(100)")
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100, columnDefinition = "VARCHAR2(100)")
    private String apellidos;

    @Column(name = "fechaNacimiento", nullable = true, columnDefinition = "DATE")
    private LocalDate fechaNacimiento;
}
