package pe.com.rebolsysBackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "cliente",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "cliente_perfil_unq",
          columnNames = {"idPerfil"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idCliente", nullable = false, columnDefinition = "NUMBER")
  private Long idCliente;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "idPerfil",
      columnDefinition = "NUMBER",
      nullable = false,
      foreignKey = @ForeignKey(name = "cliente_perfil_fk"))
  private Perfil perfil;

  @Column(name = "restriccionesMedicas", length = 500, columnDefinition = "VARCHAR2(500)")
  private String restriccionesMedicas;
}
