package pe.com.rebolsysBackend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.*;
import pe.com.rebolsysBackend.enums.*;

@Entity
@Table(name = "detalle_venta")
@Getter
@Setter
@NoArgsConstructor
public class DetalleVenta {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idDetalleVenta", columnDefinition = "NUMBER")
  private Long idDetalleVenta;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "idVenta", nullable = false, columnDefinition = "NUMBER")
  private Venta venta;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "idProducto", nullable = false, columnDefinition = "NUMBER")
  private Producto producto;

  @Column(name = "descripcion", columnDefinition = "VARCHAR2(300)", nullable = false)
  private String descripcion;

  @Column(name = "cantidad", columnDefinition = "NUMBER(9,0)", nullable = false)
  private Integer cantidad;

  @Column(name = "valorUnitario", columnDefinition = "NUMBER(12,4)", nullable = false)
  private BigDecimal valorUnitario;

  @Column(name = "igv", columnDefinition = "NUMBER(12,2)", nullable = false)
  private BigDecimal igv;

  @Column(
      name = "subtotal",
      columnDefinition = "NUMBER(12,2)",
      nullable = true,
      insertable = false,
      updatable = false)
  private BigDecimal subtotal;

  @Column(
      name = "total",
      columnDefinition = "NUMBER(12,2)",
      nullable = true,
      insertable = false,
      updatable = false)
  private BigDecimal total;

  @Column(name = "estadoLinea", columnDefinition = "VARCHAR2(10)", nullable = false)
  private String estadoLinea;
}
