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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
    name = "token_recuperacion",
    uniqueConstraints = { @UniqueConstraint(name = "token_unq", columnNames = { "tokenHash" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idToken", nullable = false, columnDefinition = "NUMBER")
    private Long idToken;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idUsuario",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "token_usuario_fk")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario usuario;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "tokenHash", nullable = false, length = 255, columnDefinition = "VARCHAR2(255)")
    private String tokenHash;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fechaCreacion", nullable = false, columnDefinition = "TIMESTAMP", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fechaExpiracion", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime fechaExpiracion;

    @ColumnDefault("0")
    @Column(name = "usado", nullable = false, precision = 1, scale = 0, columnDefinition = "NUMBER(1)")
    private Integer usado = 0;

    @PrePersist
    protected void completarFechaRegistro() {
        if (fechaCreacion == null) fechaCreacion = LocalDateTime.now();
    }
}
