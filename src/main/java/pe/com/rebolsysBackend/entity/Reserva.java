package pe.com.rebolsysBackend.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.*;
import pe.com.rebolsysBackend.enums.*;

@Entity
@Table(name = "reserva")
@Getter
@Setter
@NoArgsConstructor
public class Reserva {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idReserva", columnDefinition = "NUMBER")
  private Long idReserva;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "idPerfil", nullable = false, columnDefinition = "NUMBER")
  private Perfil cliente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "idUsuario", nullable = false, columnDefinition = "NUMBER")
  private Usuario usuario;

  @Column(name = "claveRegistro", columnDefinition = "VARCHAR2(64)", nullable = false)
  private String claveRegistro;

  @Column(name = "firmaSolicitud", columnDefinition = "VARCHAR2(64)", nullable = false)
  private String firmaSolicitud;

  @Column(name = "fechaRegistro", columnDefinition = "TIMESTAMP", nullable = false)
  private LocalDateTime fechaRegistro;

  @OneToMany(
      mappedBy = "reserva",
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @OrderBy("idDetalleReserva ASC")
  private List<DetalleReserva> detalles = new ArrayList<>();

  public void agregarDetalle(DetalleReserva d) {
    detalles.add(d);
    d.setReserva(this);
  }

  @PrePersist
  void fecha() {
    if (fechaRegistro == null)
      fechaRegistro = LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
  }
}
