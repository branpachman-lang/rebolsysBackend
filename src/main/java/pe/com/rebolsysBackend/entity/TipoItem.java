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
    name = "tipo_item",
    uniqueConstraints = { @UniqueConstraint(name = "tipo_item_nombre_unq", columnNames = { "nombreItem" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TipoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTipoItem", nullable = false, columnDefinition = "NUMBER")
    private Long idTipoItem;

    @Column(name = "nombreItem", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private String nombreItem;
}
