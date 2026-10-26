package pe.com.rebolsysBackend.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RolPermisoId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "idRol", nullable = false, columnDefinition = "NUMBER")
    private Long idRol;

    @Column(name = "idPermiso", nullable = false, columnDefinition = "NUMBER")
    private Long idPermiso;
}
