package pe.com.rebolsysBackend.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoReserva;

@Entity
@Table(
    name = "reserva",
    uniqueConstraints = @UniqueConstraint(name = "reserva_clave_uq", columnNames = "claveRegistro")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idReserva", columnDefinition = "NUMBER")
    private Long idReserva;

    @Column(name = "claveRegistro", nullable = false, length = 64, columnDefinition = "VARCHAR2(64)")
    private String claveRegistro;

    @Column(name = "firmaSolicitud", nullable = false, length = 64, columnDefinition = "VARCHAR2(64)")
    private String firmaSolicitud;

    @Column(name = "fechaRegistro", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idCliente",
        nullable = false,
        columnDefinition = "NUMBER",
        foreignKey = @ForeignKey(name = "reserva_cliente_fk")
    )
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idUsuario",
        nullable = false,
        columnDefinition = "NUMBER",
        foreignKey = @ForeignKey(name = "reserva_usuario_fk")
    )
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "estadoReserva", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private EstadoReserva estadoReserva = EstadoReserva.CONFIRMADA;

    // Los detalles cancelados conservan sus consumos y comprobantes.
    @OneToMany(mappedBy = "reserva", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @OrderBy("idDetalleReserva ASC")
    private List<DetalleReserva> detalles = new ArrayList<>();

    public void agregarDetalle(DetalleReserva detalle) {
        detalles.add(detalle);
        detalle.setReserva(this);
    }

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) fechaRegistro = LocalDateTime.now();
        if (estadoReserva == null) estadoReserva = EstadoReserva.CONFIRMADA;
    }
}
