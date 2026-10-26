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
    name = "tipo_documento",
    uniqueConstraints = { @UniqueConstraint(name = "tipo_documento_nombre_unq", columnNames = { "nombreDocumento" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTipoDocumento", nullable = false, columnDefinition = "NUMBER")
    private Long idTipoDocumento;

    @Column(name = "nombreDocumento", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private String nombreDocumento;

    @Column(name = "codigoSunat", nullable = true, length = 2, columnDefinition = "VARCHAR2(2)")
    private String codigoSunat;
}
