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
    name = "tipo_comprobante",
    uniqueConstraints = {
        @UniqueConstraint(name = "tipo_comprobante_codigo_unq", columnNames = { "codigo" }),
        @UniqueConstraint(name = "tipo_comprobante_nombre_unq", columnNames = { "nombreComprobante" }),
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoComprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTipoComprobante", nullable = false, columnDefinition = "NUMBER")
    private Long idTipoComprobante;

    @Column(name = "codigo", nullable = false, length = 20, columnDefinition = "VARCHAR2(20)")
    private String codigo;

    @Column(name = "nombreComprobante", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private String nombreComprobante;

    @Column(name = "codigoSunat", nullable = true, length = 2, columnDefinition = "VARCHAR2(2)")
    private String codigoSunat;
}
