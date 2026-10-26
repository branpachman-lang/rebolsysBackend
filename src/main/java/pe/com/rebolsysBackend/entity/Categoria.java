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
@Table(
    name = "categoria",
    uniqueConstraints = { @UniqueConstraint(name = "categoria_nombre_unq", columnNames = { "nombreCategoria" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idCategoria", nullable = false, columnDefinition = "NUMBER")
    private Long idCategoria;

    @Column(name = "nombreCategoria", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private String nombreCategoria;
}
