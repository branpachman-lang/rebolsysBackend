package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.exigir;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import pe.com.rebolsysBackend.config.Acceso;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.exception.RecursoNoEncontradoException;

/** El catálogo se deriva de las rutas protegidas. Los permisos individuales se guardan en un rol interno. */
@Service
@RequiredArgsConstructor
public class AccesosService {
  public static final String GERENTE = "GERENTE";
  public static final String RECEPCIONISTA = "RECEPCIONISTA";
  private final ApplicationContext contexto;
  private final EntityManager em;
  private final JdbcTemplate jdbc;

  public record PerfilAcceso(List<Long> idsRoles, List<String> roles, List<String> individuales, List<String> permisos) {
    public boolean gerente() { return roles.contains(GERENTE); }
    public boolean operaCaja() { return gerente() || roles.contains(RECEPCIONISTA); }
  }

  public record Funcion(String codigo, String modulo, String accion) {}

  public static String[] regla(HandlerMethod handler) {
    var acceso = handler.getMethodAnnotation(Acceso.class);
    if (acceso == null) acceso = handler.getBeanType().getAnnotation(Acceso.class);
    return acceso == null ? new String[0] : acceso.value();
  }

  public List<Funcion> catalogo() {
    return contexto.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class).getHandlerMethods()
        .values().stream().flatMap(h -> Arrays.stream(regla(h))).filter(c -> !c.startsWith("@"))
        .distinct().sorted().map(c -> new Funcion(c, c.split(":", 2)[0], c.split(":", 2)[1])).toList();
  }

  public PerfilAcceso perfil(Long usuario) {
    var roles = jdbc.query("""
        select r.idRol, r.nombreRol from usuario_rol ur join rol r on r.idRol=ur.idRol
        where ur.idUsuario=? order by r.nombreRol
        """, (rs, n) -> new Rol(rs.getLong(1), rs.getString(2)), usuario);
    var externos = roles.stream().filter(r -> !r.getNombreRol().startsWith("@")).toList();
    var efectivos = jdbc.queryForList("""
        select distinct m.nombreModulo || ':' || p.accion as codigo
        from usuario_rol ur join rol_permiso rp on rp.idRol=ur.idRol
        join permiso p on p.idPermiso=rp.idPermiso join modulo m on m.idModulo=p.idModulo
        where ur.idUsuario=? order by codigo
        """, String.class, usuario);
    var privados = roles.stream().filter(r -> r.getNombreRol().equals(rolPrivado(usuario))).findFirst();
    var nombres = externos.stream().map(Rol::getNombreRol).toList();
    return new PerfilAcceso(externos.stream().map(Rol::getIdRol).toList(), nombres,
        privados.map(r -> permisosRol(r.getIdRol())).orElse(List.of()),
        nombres.contains(GERENTE) ? catalogo().stream().map(Funcion::codigo).toList() : efectivos);
  }

  public List<Rol> roles() {
    return em.createQuery("select r from Rol r where r.nombreRol not like '@%' escape '!' order by r.nombreRol", Rol.class)
        .getResultList();
  }

  public List<String> permisosRol(Long rol) {
    return jdbc.queryForList("""
        select m.nombreModulo || ':' || p.accion as codigo from rol_permiso rp
        join permiso p on p.idPermiso=rp.idPermiso join modulo m on m.idModulo=p.idModulo
        where rp.idRol=? order by codigo
        """, String.class, rol);
  }

  public boolean esGerente(Long usuario) {
    return jdbc.queryForObject("""
        select count(*) from usuario_rol ur join rol r on r.idRol=ur.idRol
        where ur.idUsuario=? and r.nombreRol=?
        """, Long.class, usuario, GERENTE) > 0;
  }

  public long usuariosAsignados(Long rol) {
    return jdbc.queryForObject("select count(*) from usuario_rol where idRol=?", Long.class, rol);
  }

  public void eliminarRol(Rol rol) {
    exigir(usuariosAsignados(rol.getIdRol()) == 0, "Retira el rol de los usuarios asignados antes de eliminarlo");
    jdbc.update("delete from rol_permiso where idRol=?", rol.getIdRol());
    em.remove(rol);
  }

  public Rol buscarRol(String nombre) {
    return em.createQuery("select r from Rol r where r.nombreRol=:nombre", Rol.class)
        .setParameter("nombre", nombre).getResultList().stream().findFirst().orElse(null);
  }

  public Rol crearRol(String nombre) {
    var rol = new Rol(null, nombre);
    em.persist(rol);
    return rol;
  }

  public Rol bloquearGerencia() {
    return em.createQuery("select r from Rol r where r.nombreRol=:nombre", Rol.class)
        .setParameter("nombre", GERENTE).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList().stream()
        .findFirst().orElseThrow(() -> new RecursoNoEncontradoException("Falta configurar el rol GERENTE"));
  }

  public void asignar(Long usuario, Set<Long> idsRoles, Set<String> individuales) {
    validar(individuales);
    for (Long id : idsRoles) {
      var rol = em.find(Rol.class, id);
      exigir(rol != null && !rol.getNombreRol().equals(GERENTE) && !rol.getNombreRol().startsWith("@"),
          "El rol seleccionado no se puede asignar");
    }
    var interno = buscarRol(rolPrivado(usuario));
    if (!individuales.isEmpty() && interno == null) interno = crearRol(rolPrivado(usuario));
    jdbc.update("delete from usuario_rol where idUsuario=?", usuario);
    for (Long rol : idsRoles) vincular(usuario, rol);
    if (interno != null) {
      permisos(interno.getIdRol(), individuales);
      if (!individuales.isEmpty()) vincular(usuario, interno.getIdRol());
    }
  }

  public void vincular(Long usuario, Long rol) {
    jdbc.update("insert into usuario_rol (idUsuario,idRol) values (?,?)", usuario, rol);
  }

  public void permisos(Long rol, Set<String> codigos) {
    validar(codigos);
    jdbc.update("delete from rol_permiso where idRol=?", rol);
    for (String codigo : new TreeSet<>(codigos)) {
      var partes = codigo.split(":", 2);
      var modulo = em.createQuery("select m from Modulo m where m.nombreModulo=:nombre", Modulo.class)
          .setParameter("nombre", partes[0]).getResultList().stream().findFirst().orElse(null);
      if (modulo == null) {
        modulo = new Modulo(null, partes[0], null);
        em.persist(modulo);
      }
      var permiso = em.createQuery("select p from Permiso p where p.modulo=:modulo and p.accion=:accion", Permiso.class)
          .setParameter("modulo", modulo).setParameter("accion", partes[1]).getResultList().stream().findFirst().orElse(null);
      if (permiso == null) {
        permiso = new Permiso(null, modulo, partes[1]);
        em.persist(permiso);
      }
      jdbc.update("insert into rol_permiso (idRol,idPermiso) values (?,?)", rol, permiso.getIdPermiso());
    }
  }

  private void validar(Set<String> permisos) {
    var conocidos = catalogo().stream().map(Funcion::codigo).collect(java.util.stream.Collectors.toSet());
    exigir(conocidos.containsAll(permisos), "La selección contiene funcionalidades desconocidas");
  }

  private String rolPrivado(Long usuario) {
    return "@U" + usuario;
  }
}
