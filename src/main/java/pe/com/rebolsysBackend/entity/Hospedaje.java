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
    name = "hospedaje",
    uniqueConstraints = { @UniqueConstraint(name = "hospedaje_ruc_unq", columnNames = { "ruc" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Hospedaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idHospedaje", nullable = false, columnDefinition = "NUMBER")
    private Long idHospedaje;

    @Column(name = "nombre", nullable = false, length = 150, columnDefinition = "VARCHAR2(150)")
    private String nombre;

    @Column(name = "ruc", nullable = false, length = 11, columnDefinition = "VARCHAR2(11)")
    private String ruc;

    @Column(name = "direccion", nullable = true, length = 200, columnDefinition = "VARCHAR2(200)")
    private String direccion;

    @Column(name = "logoRuta", nullable = true, length = 300, columnDefinition = "VARCHAR2(300)")
    private String logoRuta;
}
