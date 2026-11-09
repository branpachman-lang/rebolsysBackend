package pe.com.rebolsysBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import pe.com.rebolsysBackend.enums.EstadoProducto;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idProducto", nullable = false, columnDefinition = "NUMBER")
    private Long idProducto;

    @Column(name = "nombreProducto", nullable = false, length = 100, columnDefinition = "VARCHAR2(100)")
    private String nombreProducto;

    @Column(name = "precioActual", nullable = false, precision = 10, scale = 2, columnDefinition = "NUMBER(10,2)")
    private BigDecimal precioActual;

    @ColumnDefault("0")
    @Column(name = "stock", nullable = false, precision = 9, scale = 0, columnDefinition = "NUMBER(9,0)")
    private Integer stock = 0;

    @ColumnDefault("0")
    @Column(name = "stockMinimo", nullable = false, precision = 9, scale = 0, columnDefinition = "NUMBER(9,0)")
    private Integer stockMinimo = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idCategoria",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "producto_categoria_fk")
    )
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "estadoProducto", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private EstadoProducto estadoProducto;

    @Column(name = "imagenRuta", length = 300, columnDefinition = "VARCHAR2(300)")
    private String imagenRuta;

    @Column(name = "descripcion", length = 500, columnDefinition = "VARCHAR2(500 CHAR)")
    private String descripcion;
}
