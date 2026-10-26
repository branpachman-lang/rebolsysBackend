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
    name = "tipo_credencial",
    uniqueConstraints = { @UniqueConstraint(name = "tp_nombre_unq", columnNames = { "nombreCredencial" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoCredencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTipoCredencial", nullable = false, columnDefinition = "NUMBER")
    private Long idTipoCredencial;

    @Column(name = "nombreCredencial", nullable = false, length = 50, columnDefinition = "VARCHAR2(50)")
    private String nombreCredencial;
}
