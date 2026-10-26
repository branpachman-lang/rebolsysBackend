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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import pe.com.rebolsysBackend.enums.AccionAuditoria;

@Entity
@Table(name = "auditoria_general")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaGeneral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idAuditoria", nullable = false, columnDefinition = "NUMBER")
    private Long idAuditoria;

    @Column(name = "nombreTabla", nullable = false, length = 50, columnDefinition = "VARCHAR2(50)")
    private String nombreTabla;

    @Column(name = "idRegistro", nullable = false, columnDefinition = "NUMBER")
    private Long idRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 10, columnDefinition = "VARCHAR2(10)")
    private AccionAuditoria accion;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name = "idUsuario",
        columnDefinition = "NUMBER",
        nullable = true,
        foreignKey = @ForeignKey(name = "auditoria_usuario_fk")
    )
    private Usuario usuario;

    @Lob
    @Column(name = "datosAnteriores", nullable = true, columnDefinition = "CLOB")
    private String datosAnteriores;

    @Lob
    @Column(name = "datosNuevos", nullable = true, columnDefinition = "CLOB")
    private String datosNuevos;

    @Column(name = "ipOrigen", nullable = true, length = 45, columnDefinition = "VARCHAR2(45)")
    private String ipOrigen;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "fechaHora", nullable = false, columnDefinition = "TIMESTAMP", updatable = false)
    private LocalDateTime fechaHora;

    @PrePersist
    protected void completarFechaRegistro() {
        if (fechaHora == null) fechaHora = LocalDateTime.now();
    }
}
