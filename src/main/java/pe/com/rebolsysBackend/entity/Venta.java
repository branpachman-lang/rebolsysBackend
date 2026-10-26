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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.ModoVenta;

@Entity
@Table(name = "venta", uniqueConstraints = @UniqueConstraint(name = "venta_clave_uq", columnNames = "claveRegistro"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idVenta", nullable = false, columnDefinition = "NUMBER")
    private Long idVenta;

    @Column(name = "claveRegistro", nullable = false, length = 64, columnDefinition = "VARCHAR2(64)")
    private String claveRegistro;

    @Column(name = "firmaSolicitud", nullable = false, length = 64, columnDefinition = "VARCHAR2(64)")
    private String firmaSolicitud;

    @Column(name = "fechaVenta", nullable = false, columnDefinition = "TIMESTAMP", updatable = false)
    private LocalDateTime fechaVenta;

    @Enumerated(EnumType.STRING)
    @Column(name = "modoVenta", nullable = false, length = 20, columnDefinition = "VARCHAR2(20)")
    private ModoVenta modoVenta;

    @Column(name = "compradorNombre", length = 200, columnDefinition = "VARCHAR2(200)")
    private String compradorNombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "idCliente", columnDefinition = "NUMBER", foreignKey = @ForeignKey(name = "venta_cliente_fk"))
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "idDetalleReserva",
        columnDefinition = "NUMBER",
        foreignKey = @ForeignKey(name = "venta_detalle_reserva_fk")
    )
    private DetalleReserva detalleReserva;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idUsuario",
        nullable = false,
        columnDefinition = "NUMBER",
        foreignKey = @ForeignKey(name = "venta_usuario_fk")
    )
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "idComprobante",
        columnDefinition = "NUMBER",
        foreignKey = @ForeignKey(name = "venta_comprobante_fk")
    )
    private Comprobante comprobante;

    @PrePersist
    protected void completarFecha() {
        if (fechaVenta == null) fechaVenta = LocalDateTime.now();
    }
}
