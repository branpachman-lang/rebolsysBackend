package pe.com.rebolsysBackend.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.entity.id.UsuarioRolId;

@Entity
@Table(name = "usuario_rol")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRol {

    @EmbeddedId
    private UsuarioRolId id;

    @MapsId("idUsuario")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idUsuario",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "ur_usuario_fk")
    )
    private Usuario usuario;

    @MapsId("idRol")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idRol",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "ur_rol_fk")
    )
    private Rol rol;
}
