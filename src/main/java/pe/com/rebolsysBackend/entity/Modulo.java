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
    name = "modulo",
    uniqueConstraints = { @UniqueConstraint(name = "modulo_nombre_unq", columnNames = { "nombreModulo" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Modulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idModulo", nullable = false, columnDefinition = "NUMBER")
    private Long idModulo;

    @Column(name = "nombreModulo", nullable = false, length = 50, columnDefinition = "VARCHAR2(50)")
    private String nombreModulo;

    @Column(name = "ruta", nullable = true, length = 100, columnDefinition = "VARCHAR2(100)")
    private String ruta;
}
