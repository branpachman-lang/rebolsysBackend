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
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import pe.com.rebolsysBackend.enums.EstadoComprobante;

@Entity
@Table(
    name = "comprobante",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "comprobante_numero_unq",
          columnNames = {"idTipoComprobante", "serie", "numeroComprobante"}),
      @UniqueConstraint(
          name = "comprobante_idem_unq",
          columnNames = {"claveIdempotencia"}),
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Comprobante {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idComprobante", nullable = false, columnDefinition = "NUMBER")
  private Long idComprobante;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(
      name = "idPerfilReceptor",
      columnDefinition = "NUMBER",
      nullable = true,
      foreignKey = @ForeignKey(name = "comprobante_cliente_fk"))
  private Perfil cliente;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "idTipoComprobante",
      columnDefinition = "NUMBER",
      nullable = false,
      foreignKey = @ForeignKey(name = "comprobante_tipo_fk"))
  private TipoComprobante tipoComprobante;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "idTurno",
      columnDefinition = "NUMBER",
      nullable = false,
      foreignKey = @ForeignKey(name = "comprobante_turno_fk"))
  private TurnoCaja turnoCaja;

  @Column(name = "serie", nullable = false, length = 10, columnDefinition = "VARCHAR2(10)")
  private String serie;

  @Column(
      name = "numeroComprobante",
      nullable = true,
      length = 20,
      columnDefinition = "VARCHAR2(20)")
  private String numeroComprobante;

  @ColumnDefault("CURRENT_TIMESTAMP")
  @Column(name = "fechaEmision", nullable = true, columnDefinition = "TIMESTAMP")
  private LocalDateTime fechaEmision;

  @Column(
      name = "subtotal",
      nullable = false,
      precision = 12,
      scale = 2,
      columnDefinition = "NUMBER(12,2)")
  private BigDecimal subtotal;

  @Column(
      name = "igv",
      nullable = false,
      precision = 12,
      scale = 2,
      columnDefinition = "NUMBER(12,2)")
  private BigDecimal igv;

  @Column(
      name = "totalPagar",
      nullable = false,
      precision = 12,
      scale = 2,
      columnDefinition = "NUMBER(12,2)")
  private BigDecimal totalPagar;

  @Column(name = "receptorTipoDoc", nullable = true, length = 10, columnDefinition = "VARCHAR2(10)")
  private String receptorTipoDoc;

  @Column(
      name = "receptorNumeroDoc",
      nullable = true,
      length = 50,
      columnDefinition = "VARCHAR2(50)")
  private String receptorNumeroDoc;

  @Column(
      name = "receptorNombre",
      nullable = true,
      length = 250,
      columnDefinition = "VARCHAR2(250)")
  private String receptorNombre;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(
      name = "idComprobanteOrigen",
      columnDefinition = "NUMBER",
      nullable = true,
      foreignKey = @ForeignKey(name = "comprobante_origen_fk"))
  private Comprobante comprobanteOrigen;

  @Enumerated(EnumType.STRING)
  @Column(
      name = "estadoComprobante",
      nullable = false,
      length = 15,
      columnDefinition = "VARCHAR2(15)")
  private EstadoComprobante estadoComprobante;

  @Column(name = "hashCpe", nullable = true, length = 100, columnDefinition = "VARCHAR2(100)")
  private String hashCpe;

  @Column(
      name = "sunatCodigoRespuesta",
      nullable = true,
      length = 10,
      columnDefinition = "VARCHAR2(10)")
  private String sunatCodigoRespuesta;

  @Column(name = "sunatMensaje", nullable = true, length = 500, columnDefinition = "VARCHAR2(500)")
  private String sunatMensaje;

  @Column(
      name = "claveIdempotencia",
      nullable = true,
      length = 64,
      columnDefinition = "VARCHAR2(64)")
  private String claveIdempotencia;

  // Vista de la FK compuesta; las columnas se escriben mediante los campos anteriores.
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumns(
      value = {
        @JoinColumn(
            name = "idTipoComprobante",
            columnDefinition = "NUMBER",
            referencedColumnName = "idTipoComprobante",
            insertable = false,
            updatable = false),
        @JoinColumn(
            name = "serie",
            columnDefinition = "VARCHAR2(10)",
            referencedColumnName = "serie",
            insertable = false,
            updatable = false),
      },
      foreignKey = @ForeignKey(name = "comprobante_serie_fk"))
  private SerieComprobante serieComprobante;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "idReserva", nullable = true, columnDefinition = "NUMBER")
  private Reserva reserva;

  @ManyToOne(fetch = FetchType.LAZY, optional = true)
  @JoinColumn(name = "idVenta", nullable = true, columnDefinition = "NUMBER")
  private Venta venta;
}
