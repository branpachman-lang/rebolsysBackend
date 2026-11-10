package pe.com.rebolsysBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Convert;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import pe.com.rebolsysBackend.enums.EstadoConservacion;
import pe.com.rebolsysBackend.entity.converter.CondicionItemConverter;

@Entity
@Table(
    name = "detalle_informe",
    uniqueConstraints = {
        @UniqueConstraint(name = "di_informe_item_unq", columnNames = { "idInforme", "idItem" }),
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleInforme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idDetalleInforme", nullable = false, columnDefinition = "NUMBER")
    private Long idDetalleInforme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idInforme",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "di_informe_fk")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private InformeRevision informeRevision;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idItem", nullable = false, columnDefinition = "NUMBER")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idHabitacion", nullable = false, columnDefinition = "NUMBER")
    private Habitacion habitacion;

    @Convert(converter = CondicionItemConverter.class)
    @Column(name = "condicionAnterior", columnDefinition = "VARCHAR2(30)")
    private EstadoConservacion condicionAnterior;

    @Convert(converter = CondicionItemConverter.class)
    @Column(name = "estadoEncontrado", nullable = false, length = 30, columnDefinition = "VARCHAR2(30)")
    private EstadoConservacion estadoEncontrado;

    @Column(name = "evidenciaRuta", nullable = true, length = 300, columnDefinition = "VARCHAR2(300)")
    private String evidenciaRuta;

    @Column(name = "evidenciaSha256", nullable = true, length = 64, columnDefinition = "CHAR(64)")
    private String evidenciaSha256;

    @Column(name = "observacion", length = 1000, columnDefinition = "VARCHAR2(1000)")
    private String observaciones;
}
