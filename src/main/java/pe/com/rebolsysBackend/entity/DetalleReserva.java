package pe.com.rebolsysBackend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.*;
import pe.com.rebolsysBackend.enums.*;

@Entity
@Table(name = "detalle_reserva")
@Getter
@Setter
@NoArgsConstructor
public class DetalleReserva {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idDetalleReserva", columnDefinition = "NUMBER")
  private Long idDetalleReserva;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "idReserva", nullable = false, columnDefinition = "NUMBER")
  private Reserva reserva;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "idHabitacion", nullable = false, columnDefinition = "NUMBER")
  private Habitacion habitacion;

  @Column(name = "fechaIngreso", columnDefinition = "TIMESTAMP", nullable = false)
  private LocalDateTime fechaIngreso;

  @Column(name = "fechaSalida", columnDefinition = "TIMESTAMP", nullable = false)
  private LocalDateTime fechaSalida;

  @Column(name = "fechaIngresoReal", columnDefinition = "TIMESTAMP", nullable = true)
  private LocalDateTime fechaIngresoReal;

  @Column(name = "fechaSalidaReal", columnDefinition = "TIMESTAMP", nullable = true)
  private LocalDateTime fechaSalidaReal;

  @Column(name = "costoEstadia", columnDefinition = "NUMBER(12,2)", nullable = false)
  private BigDecimal costoEstadia;

  @Enumerated(EnumType.STRING)
  @Column(name = "estadoDetalle", columnDefinition = "VARCHAR2(20)", nullable = false)
  private EstadoReserva estadoDetalle;
}
