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
    name = "forma_pago",
    uniqueConstraints = { @UniqueConstraint(name = "forma_pago_nombre_unq", columnNames = { "nombrePago" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FormaPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idFormaPago", nullable = false, columnDefinition = "NUMBER")
    private Long idFormaPago;

    @Column(name = "nombrePago", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private String nombrePago;
}
