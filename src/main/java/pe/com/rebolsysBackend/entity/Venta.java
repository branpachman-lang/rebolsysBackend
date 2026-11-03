package pe.com.rebolsysBackend.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.*;
import pe.com.rebolsysBackend.enums.*;

@Entity
@Table(name = "venta")
@Getter
@Setter
@NoArgsConstructor
public class Venta {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idVenta", columnDefinition = "NUMBER")
  private Long idVenta;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "idClienteComprador", nullable = true, columnDefinition = "NUMBER")
  private Cliente clienteComprador;

  @Column(name = "compradorNombreLibre", columnDefinition = "VARCHAR2(250)", nullable = true)
  private String compradorNombreLibre;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "idDetalleReserva", nullable = true, columnDefinition = "NUMBER")
  private DetalleReserva detalleReserva;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "idUsuario", nullable = false, columnDefinition = "NUMBER")
  private Usuario usuario;

  @Enumerated(EnumType.STRING)
  @Column(name = "modalidadCobro", columnDefinition = "VARCHAR2(10)", nullable = false)
  private ModalidadCobro modalidadCobro;

  @Enumerated(EnumType.STRING)
  @Column(name = "estadoVenta", columnDefinition = "VARCHAR2(15)", nullable = false)
  private EstadoVenta estadoVenta;

  @Column(name = "fechaRegistro", columnDefinition = "TIMESTAMP", nullable = false)
  private LocalDateTime fechaRegistro;

  @Column(name = "fechaConfirmacion", columnDefinition = "TIMESTAMP", nullable = true)
  private LocalDateTime fechaConfirmacion;

  @Column(name = "claveRegistro", columnDefinition = "VARCHAR2(64)", nullable = false)
  private String claveRegistro;

  @Column(name = "firmaSolicitud", columnDefinition = "VARCHAR2(64)", nullable = false)
  private String firmaSolicitud;

  @OneToMany(
      mappedBy = "venta",
      orphanRemoval = true,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @OrderBy("idDetalleVenta ASC")
  private List<DetalleVenta> detalles = new ArrayList<>();

  @PrePersist
  void fecha() {
    if (fechaRegistro == null)
      fechaRegistro = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
  }
}
