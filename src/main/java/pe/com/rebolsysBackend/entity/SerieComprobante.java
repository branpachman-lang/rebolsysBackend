package pe.com.rebolsysBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import pe.com.rebolsysBackend.entity.id.SerieComprobanteId;

@Entity
@Table(name = "serie_comprobante")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SerieComprobante {

    @EmbeddedId
    private SerieComprobanteId id;

    @ColumnDefault("1")
    @Column(name = "siguienteNumero", nullable = false, precision = 10, scale = 0, columnDefinition = "NUMBER(10)")
    private Long siguienteNumero = 1L;

    @MapsId("idTipoComprobante")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "idTipoComprobante",
        columnDefinition = "NUMBER",
        nullable = false,
        foreignKey = @ForeignKey(name = "sc_tipo_fk")
    )
    private TipoComprobante tipoComprobante;
}
