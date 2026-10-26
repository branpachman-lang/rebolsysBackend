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
    name = "tipo_ocupante",
    uniqueConstraints = { @UniqueConstraint(name = "tipo_ocupante_nombre_unq", columnNames = { "nombreTipo" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoOcupante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTipoOcupante", nullable = false, columnDefinition = "NUMBER")
    private Long idTipoOcupante;

    @Column(name = "nombreTipo", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private String nombreTipo;
}
