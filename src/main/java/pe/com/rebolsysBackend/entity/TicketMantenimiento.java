package pe.com.rebolsysBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import pe.com.rebolsysBackend.entity.converter.EstadoTicketConverter;
import pe.com.rebolsysBackend.enums.EstadoTicket;
import pe.com.rebolsysBackend.enums.TipoAccionMantenimiento;

@Entity
@Table(
    name = "ticket_mantenimiento",
    uniqueConstraints = { @UniqueConstraint(name = "ticket_detalle_unq", columnNames = { "idDetalleInforme" }) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idTicket", nullable = false, columnDefinition = "NUMBER")
    private Long idTicket;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idDetalleInforme",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "ticket_detalle_fk")
    )
    private DetalleInforme detalleInforme;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name = "idEmpleado",
        columnDefinition = "NUMBER",
        nullable = true,
        foreignKey = @ForeignKey(name = "ticket_empleado_fk")
    )
    private Empleado empleado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipoAccion", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private TipoAccionMantenimiento tipoAccion;

    @Convert(converter = EstadoTicketConverter.class)
    @Column(name = "estadoTicket", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private EstadoTicket estadoTicket;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fechaGeneracion", nullable = false, columnDefinition = "TIMESTAMP", updatable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "fechaResolucion", nullable = true, columnDefinition = "TIMESTAMP")
    private LocalDateTime fechaResolucion;

    @Column(name = "costoReparacion", nullable = true, precision = 10, scale = 2, columnDefinition = "NUMBER(10,2)")
    private BigDecimal costoReparacion;

    @Column(name = "evidenciaRuta", nullable = true, length = 300, columnDefinition = "VARCHAR2(300)")
    private String evidenciaRuta;

    @Column(name = "evidenciaSha256", nullable = true, length = 64, columnDefinition = "CHAR(64)")
    private String evidenciaSha256;

    @Column(name = "observacionTecnica", nullable = true, length = 1000, columnDefinition = "VARCHAR2(1000)")
    private String observacionTecnica;

    @PrePersist
    protected void completarFechaRegistro() {
        if (fechaGeneracion == null) fechaGeneracion = LocalDateTime.now();
    }
}
