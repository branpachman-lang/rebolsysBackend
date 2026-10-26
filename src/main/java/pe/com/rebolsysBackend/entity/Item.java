package pe.com.rebolsysBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import pe.com.rebolsysBackend.entity.converter.EstadoItemConverter;
import pe.com.rebolsysBackend.enums.EstadoItem;

@Entity
@Table(name = "item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idItem", nullable = false, columnDefinition = "NUMBER")
    private Long idItem;

    @Column(name = "nombreItem", nullable = false, length = 100, columnDefinition = "VARCHAR2(100)")
    private String nombreItem;

    @Convert(converter = EstadoItemConverter.class)
    @ColumnDefault("'Activo'")
    @Column(name = "estadoItem", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private EstadoItem estadoItem = EstadoItem.ACTIVO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idTipoItem",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "item_tipo_fk")
    )
    private TipoItem tipoItem;
}
