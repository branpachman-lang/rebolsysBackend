package pe.com.rebolsysBackend.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.*;
import pe.com.rebolsysBackend.enums.*;

@Entity
@Table(name = "movimiento_inventario")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoInventario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idMovimiento", columnDefinition = "NUMBER")
  private Long idMovimiento;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "idProducto", nullable = true, columnDefinition = "NUMBER")
  private Producto producto;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "idItem", nullable = true, columnDefinition = "NUMBER")
  private Item item;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "idDetalleVenta", nullable = true, columnDefinition = "NUMBER")
  private DetalleVenta detalleVenta;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "idDetalleHabitacion", nullable = true, columnDefinition = "NUMBER")
  private DetalleHabitacion detalleHabitacion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "idTipoMovimientoAlmacen", nullable = false, columnDefinition = "NUMBER")
  private TipoMovimientoAlmacen tipoMovimientoAlmacen;

  @Column(name = "cantidad", columnDefinition = "NUMBER(9,0)", nullable = false)
  private Integer cantidad;

  @Column(name = "stockResultante", columnDefinition = "NUMBER(9,0)", nullable = false)
  private Integer stockResultante;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "idUsuario", nullable = false, columnDefinition = "NUMBER")
  private Usuario usuario;

  @Column(name = "observacion", columnDefinition = "VARCHAR2(300)", nullable = true)
  private String observacion;

  @Column(name = "fechaMovimiento", columnDefinition = "TIMESTAMP", nullable = false)
  private LocalDateTime fechaMovimiento;

  @Column(name = "claveOperacion", columnDefinition = "VARCHAR2(64)", nullable = false)
  private String claveOperacion;

  @PrePersist
  void fecha() {
    if (fechaMovimiento == null)
      fechaMovimiento = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
  }
}
