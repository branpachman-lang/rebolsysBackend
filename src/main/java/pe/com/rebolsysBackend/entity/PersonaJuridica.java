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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "persona_juridica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonaJuridica {

    @Id
    @Column(name = "idPerfil", nullable = false, columnDefinition = "NUMBER")
    private Long idPerfil;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idPerfil",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "empresa_perfil_fk")
    )
    private Perfil perfil;

    @Column(name = "razonSocial", nullable = false, length = 250, columnDefinition = "VARCHAR2(250)")
    private String razonSocial;

    @Column(name = "direccionFiscal", nullable = true, length = 200, columnDefinition = "VARCHAR2(200)")
    private String direccionFiscal;
}
