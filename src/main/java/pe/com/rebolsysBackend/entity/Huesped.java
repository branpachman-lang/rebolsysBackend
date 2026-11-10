package pe.com.rebolsysBackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "huesped")
@Getter
@Setter
@NoArgsConstructor
public class Huesped {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "idHuesped", columnDefinition = "NUMBER")
  private Long idHuesped;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "idPerfil", nullable = false, unique = true, columnDefinition = "NUMBER")
  private Perfil perfil;

  @Column(name = "restriccionesMedicas", length = 500, columnDefinition = "VARCHAR2(500)")
  private String restriccionesMedicas;
}
