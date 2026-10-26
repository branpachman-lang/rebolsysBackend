package pe.com.rebolsysBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(
    name = "tipo_habitacion",
    uniqueConstraints = { @UniqueConstraint(name = "tipo_habitacion_nombre_unq", columnNames = { "nombreTipo" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoHabitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTipoHabitacion", nullable = false, columnDefinition = "NUMBER")
    private Long idTipoHabitacion;

    @Column(name = "nombreTipo", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private String nombreTipo;

    @Column(name = "capacidadMax", nullable = false, precision = 2, scale = 0, columnDefinition = "NUMBER(2)")
    private Integer capacidadMax;

    @ColumnDefault("0")
    @Column(name = "precioBase", nullable = false, precision = 10, scale = 2, columnDefinition = "NUMBER(10,2)")
    private BigDecimal precioBase = new BigDecimal("0");
}
