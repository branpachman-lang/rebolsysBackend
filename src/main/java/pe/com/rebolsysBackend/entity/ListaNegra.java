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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "lista_negra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListaNegra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idListaNegra", nullable = false, columnDefinition = "NUMBER")
    private Long idListaNegra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idPerfil",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "ln_perfil_fk")
    )
    private Perfil perfil;

    @Column(name = "motivo", nullable = false, length = 500, columnDefinition = "VARCHAR2(500)")
    private String motivo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fechaRegistro", nullable = false, columnDefinition = "TIMESTAMP", updatable = false)
    private LocalDateTime fechaRegistro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idUsuario",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "ln_usuario_fk")
    )
    private Usuario usuario;

    @PrePersist
    protected void completarFechaRegistro() {
        if (fechaRegistro == null) fechaRegistro = LocalDateTime.now();
    }
}
