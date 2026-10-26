package pe.com.rebolsysBackend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(
    name = "usuario_credencial",
    uniqueConstraints = {
        @UniqueConstraint(name = "uc_usu_tipo_unq", columnNames = { "idUsuario", "idTipoCredencial" }),
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioCredencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCredencial", nullable = false, columnDefinition = "NUMBER")
    private Long idCredencial;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idUsuario",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "uc_usuario_fk")
    )
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idTipoCredencial",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "uc_tipo_credencial")
    )
    private TipoCredencial tipoCredencial;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "valorHash", nullable = true, length = 255, columnDefinition = "VARCHAR2(255)")
    private String valorHash;

    @ColumnDefault("1")
    @Column(name = "credencialTemporal", nullable = true, precision = 1, scale = 0, columnDefinition = "NUMBER(1)")
    private Integer credencialTemporal = 1;

    @ColumnDefault("0")
    @Column(name = "intentosFallidos", nullable = false, columnDefinition = "NUMBER")
    private Integer intentosFallidos = 0;

    @Column(name = "fechaBloqueo", nullable = true, columnDefinition = "TIMESTAMP")
    private LocalDateTime fechaBloqueo;
}
