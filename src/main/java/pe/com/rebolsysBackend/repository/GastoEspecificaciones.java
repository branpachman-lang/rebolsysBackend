package pe.com.rebolsysBackend.repository;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import pe.com.rebolsysBackend.dto.GastoFiltroDTO;
import pe.com.rebolsysBackend.entity.FlujoCaja;

public final class GastoEspecificaciones {
  private GastoEspecificaciones() {}

  public static Specification<FlujoCaja> filtrar(GastoFiltroDTO f) {
    return (root, query, cb) -> {
      var condiciones = new ArrayList<Predicate>();
      condiciones.add(cb.isNull(root.get("pago")));
      condiciones.add(cb.equal(root.get("tipoMovimientoCaja").get("signo"), -1));
      if (f.idTurno() != null)
        condiciones.add(cb.equal(root.get("turnoCaja").get("idTurno"), f.idTurno()));
      if (f.idUsuario() != null)
        condiciones.add(cb.equal(root.get("turnoCaja").get("usuario").get("idUsuario"), f.idUsuario()));
      if (f.idTipoMovimiento() != null)
        condiciones.add(cb.equal(root.get("tipoMovimientoCaja").get("idTipoMovimiento"), f.idTipoMovimiento()));
      if (f.desde() != null)
        condiciones.add(cb.greaterThanOrEqualTo(root.get("fechaHora"), f.desde().atStartOfDay()));
      if (f.hasta() != null)
        condiciones.add(cb.lessThan(root.get("fechaHora"), f.hasta().plusDays(1).atStartOfDay()));
      if (f.montoMinimo() != null)
        condiciones.add(cb.greaterThanOrEqualTo(root.get("monto"), f.montoMinimo()));
      if (f.montoMaximo() != null)
        condiciones.add(cb.lessThanOrEqualTo(root.get("monto"), f.montoMaximo()));
      if (!f.buscar().isBlank()) {
        String patron = "%" + f.buscar().toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        condiciones.add(cb.or(
            cb.like(cb.lower(root.get("concepto")), patron, '\\'),
            cb.like(cb.lower(root.get("documentoRespaldo")), patron, '\\'),
            cb.like(cb.lower(root.get("tipoMovimientoCaja").get("nombreMovimiento")), patron, '\\'),
            cb.like(cb.lower(root.get("turnoCaja").get("usuario").get("username")), patron, '\\')));
      }
      return cb.and(condiciones.toArray(Predicate[]::new));
    };
  }
}
