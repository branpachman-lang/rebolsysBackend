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
    name = "parametro_sistema",
    uniqueConstraints = { @UniqueConstraint(name = "parametro_clave_unq", columnNames = { "clave" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParametroSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idParametro", nullable = false, columnDefinition = "NUMBER")
    private Long idParametro;

    @Column(name = "clave", nullable = false, length = 50, columnDefinition = "VARCHAR2(50)")
    private String clave;

    @Column(name = "valor", nullable = false, length = 200, columnDefinition = "VARCHAR2(200)")
    private String valor;

    @Column(name = "descripcion", nullable = true, length = 200, columnDefinition = "VARCHAR2(200)")
    private String descripcion;
}
