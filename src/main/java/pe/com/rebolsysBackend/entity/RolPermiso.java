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
import pe.com.rebolsysBackend.entity.id.RolPermisoId;

@Entity
@Table(name = "rol_permiso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolPermiso {

    @EmbeddedId
    private RolPermisoId id;

    @MapsId("idRol")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idRol",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "rp_rol_fk")
    )
    private Rol rol;

    @MapsId("idPermiso")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idPermiso",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "rp_permiso_fk")
    )
    private Permiso permiso;
}
