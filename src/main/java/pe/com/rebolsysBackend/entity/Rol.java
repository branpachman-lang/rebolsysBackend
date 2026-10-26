package pe.com.rebolsysBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rol", uniqueConstraints = { @UniqueConstraint(name = "rol_nombre_unq", columnNames = { "nombreRol" }) })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idRol", nullable = false, columnDefinition = "NUMBER")
    private Long idRol;

    @Column(name = "nombreRol", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private String nombreRol;
}
