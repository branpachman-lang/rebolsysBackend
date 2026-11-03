package pe.com.rebolsysBackend.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import lombok.*;
import pe.com.rebolsysBackend.enums.*;

@Entity
@Table(name = "reserva_ocupante")
@Getter
@Setter
@NoArgsConstructor
public class ReservaOcupante {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idReservaOcupante", columnDefinition = "NUMBER")
  private Long idReservaOcupante;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "idDetalleReserva", nullable = false, columnDefinition = "NUMBER")
  private DetalleReserva detalleReserva;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "idCliente", nullable = false, columnDefinition = "NUMBER")
  private Cliente cliente;

  @Column(name = "idOcupanteResponsable", columnDefinition = "NUMBER", nullable = true)
  private Long idOcupanteResponsable;

  @Enumerated(EnumType.STRING)
  @Column(name = "parentescoResponsable", columnDefinition = "VARCHAR2(10)", nullable = true)
  private ParentescoResponsable parentescoResponsable;

  @Column(name = "ocupaPlazaCama", columnDefinition = "NUMBER(1)", nullable = false)
  private Integer ocupaPlazaCama;
}
