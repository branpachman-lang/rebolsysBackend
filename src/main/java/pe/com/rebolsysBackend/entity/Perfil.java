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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "perfil",
    uniqueConstraints = {
        @UniqueConstraint(name = "perfil_unq", columnNames = { "idTipoDocumento", "numeroDocumento" }),
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idPerfil", nullable = false, columnDefinition = "NUMBER")
    private Long idPerfil;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idTipoDocumento",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "perfil_tipoDocumento_fk")
    )
    private TipoDocumento tipoDocumento;

    @Column(name = "numeroDocumento", nullable = false, length = 50, columnDefinition = "VARCHAR2(50)")
    private String numeroDocumento;

    @Column(name = "correo", nullable = true, length = 100, columnDefinition = "VARCHAR2(100)")
    private String correo;

    @Column(name = "telefono", nullable = true, length = 15, columnDefinition = "VARCHAR2(15)")
    private String telefono;

    @Column(name = "esCliente", nullable = false, columnDefinition = "NUMBER(1)")
    private Integer esCliente = 0;

    public Perfil(Long idPerfil, TipoDocumento tipoDocumento, String numeroDocumento, String correo, String telefono) {
        this.idPerfil = idPerfil;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.correo = correo;
        this.telefono = telefono;
    }
}
