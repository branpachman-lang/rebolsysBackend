package pe.com.rebolsysBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.type.NumericBooleanConverter;
import pe.com.rebolsysBackend.entity.id.ReservaHuespedId;
import pe.com.rebolsysBackend.enums.ParentescoResponsable;

@Entity
@Table(name = "reserva_huesped")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservaHuesped {

    @EmbeddedId
    private ReservaHuespedId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idTipoOcupante",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "rh_tipo_fk")
    )
    private TipoOcupante tipoOcupante;

    @MapsId("idDetalleReserva")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idDetalleReserva",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "rh_detalle_reserva_fk")
    )
    private DetalleReserva detalleReserva;

    @MapsId("idHuesped")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idHuesped",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "rh_huesped_fk")
    )
    private Huesped huesped;

    @Column(name = "idHuespedResponsable", columnDefinition = "NUMBER")
    private Long idHuespedResponsable;

    @Enumerated(EnumType.STRING)
    @Column(name = "parentescoResponsable", length = 10, columnDefinition = "VARCHAR2(10)")
    private ParentescoResponsable parentescoResponsable;

    @Convert(converter = NumericBooleanConverter.class)
    @Column(name = "ocupaPlazaCama", nullable = false, precision = 1, scale = 0, columnDefinition = "NUMBER(1)")
    private Boolean ocupaPlazaCama = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns(
        value = {
            @JoinColumn(
                name = "idDetalleReserva",
                referencedColumnName = "idDetalleReserva",
                insertable = false,
                updatable = false
            ),
            @JoinColumn(
                name = "idHuespedResponsable",
                referencedColumnName = "idHuesped",
                insertable = false,
                updatable = false
            ),
        },
        foreignKey = @ForeignKey(name = "rh_responsable_fk")
    )
    private ReservaHuesped responsable;
}
