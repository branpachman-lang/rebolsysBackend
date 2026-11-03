package pe.com.rebolsysBackend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.rebolsysBackend.enums.EstadoEmisionElectronica;

@Entity
@Table(name="emision_electronica", uniqueConstraints={
    @UniqueConstraint(name="ee_comprobante_uq",columnNames="idComprobante"),
    @UniqueConstraint(name="ee_clave_uq",columnNames="claveOperacion"),
    @UniqueConstraint(name="ee_numero_uq",columnNames={"rucEmisor","tipoDocumento","serie","correlativo"})})
@Getter @Setter @NoArgsConstructor
public class EmisionElectronica {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
  @Column(name="idEmision",nullable=false,columnDefinition="NUMBER") private Long idEmision;
  @OneToOne(fetch=FetchType.LAZY,optional=false)
  @JoinColumn(name="idComprobante",nullable=false,columnDefinition="NUMBER") private Comprobante comprobante;
  @ManyToOne(fetch=FetchType.LAZY,optional=false)
  @JoinColumn(name="idUsuario",nullable=false,columnDefinition="NUMBER") private Usuario usuario;
  @Column(name="claveOperacion",nullable=false,length=64,columnDefinition="VARCHAR2(64)") private String claveOperacion;
  @Column(name="firmaSolicitud",nullable=false,length=64,columnDefinition="VARCHAR2(64)") private String firmaSolicitud;
  @Enumerated(EnumType.STRING)
  @Column(name="estado",nullable=false,length=20,columnDefinition="VARCHAR2(20)") private EstadoEmisionElectronica estado;
  @Column(name="ambiente",nullable=false,length=10,columnDefinition="VARCHAR2(10)") private String ambiente;
  @Column(name="rucEmisor",nullable=false,length=11,columnDefinition="VARCHAR2(11)") private String rucEmisor;
  @Column(name="tipoDocumento",nullable=false,length=2,columnDefinition="VARCHAR2(2)") private String tipoDocumento;
  @Column(name="serie",nullable=false,length=4,columnDefinition="VARCHAR2(4)") private String serie;
  @Column(name="correlativo",nullable=false,precision=8,scale=0,columnDefinition="NUMBER(8)") private Long correlativo;
  @Column(name="fechaEmision",nullable=false,columnDefinition="TIMESTAMP") private LocalDateTime fechaEmision;
  @Column(name="fechaActualizacion",nullable=false,columnDefinition="TIMESTAMP") private LocalDateTime fechaActualizacion;
  @Column(name="intentos",nullable=false,precision=6,scale=0,columnDefinition="NUMBER(6)") private Integer intentos;
  @Lob @Column(name="solicitud",nullable=false,columnDefinition="CLOB") private String solicitud;
  @Lob @Column(name="xmlFirmado",columnDefinition="CLOB") private String xmlFirmado;
  @Lob @Column(name="cdrZip",columnDefinition="CLOB") private String cdrZip;
  @Column(name="hashCpe",length=100,columnDefinition="VARCHAR2(100)") private String hashCpe;
  @Column(name="codigoRespuesta",length=20,columnDefinition="VARCHAR2(20)") private String codigoRespuesta;
  @Column(name="mensajeRespuesta",length=500,columnDefinition="VARCHAR2(500)") private String mensajeRespuesta;
  @Column(name="subtotal",nullable=false,precision=12,scale=2,columnDefinition="NUMBER(12,2)") private BigDecimal subtotal;
  @Column(name="igv",nullable=false,precision=12,scale=2,columnDefinition="NUMBER(12,2)") private BigDecimal igv;
  @Column(name="total",nullable=false,precision=12,scale=2,columnDefinition="NUMBER(12,2)") private BigDecimal total;
}
