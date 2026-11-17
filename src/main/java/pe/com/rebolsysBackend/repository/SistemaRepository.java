package pe.com.rebolsysBackend.repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pe.com.rebolsysBackend.entity.Hospedaje;
import pe.com.rebolsysBackend.entity.ParametroSistema;

@Repository
@RequiredArgsConstructor
public class SistemaRepository {
  private final EntityManager em;

  public Hospedaje hospedaje() {
    return em.createQuery("select h from Hospedaje h order by h.idHospedaje", Hospedaje.class)
        .setMaxResults(1).getResultList().stream().findFirst().orElse(null);
  }

  public ParametroSistema parametro(String clave) {
    return em.createQuery("select p from ParametroSistema p where p.clave = :clave", ParametroSistema.class)
        .setParameter("clave", clave).getResultList().stream().findFirst().orElse(null);
  }

  public void registrar(Hospedaje hospedaje) {
    em.persist(hospedaje);
  }

  public void registrar(ParametroSistema parametro) {
    em.persist(parametro);
  }
}
