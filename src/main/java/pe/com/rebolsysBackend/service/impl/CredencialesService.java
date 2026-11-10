package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.*;

import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.enums.EstadoUsuario;
import pe.com.rebolsysBackend.repository.TurnoCajaRepository;
import pe.com.rebolsysBackend.repository.UsuarioRepository;

@Service
@RequiredArgsConstructor
public class CredencialesService {
  private static final String TIPO = "PASSWORD";
  private static final SecureRandom ALEATORIO = new SecureRandom();
  private static final String HASH_SIMULADO = "$2a$12$7KKVNeIi1DExq/Tp6G6nJ.p86UM7PM3iagOxCym8ro50jFNipUNX.";
  private final UsuarioRepository usuarios;
  private final EntityManager em;
  private final PasswordEncoder encoder;
  private final AccesosService accesos;
  private final TurnoCajaRepository turnos;

  public record Principal(Long idUsuario, String firmaCredencial) implements java.io.Serializable {}

  public record Temporal(String contrasenaTemporal, LocalDateTime vence) {
    @Override public String toString() { return "Credencial temporal con vencimiento " + vence; }
  }

  public record Cuenta(Usuario usuario, boolean requiereCambio) {}

  @Transactional(noRollbackFor = BadCredentialsException.class)
  public Principal iniciar(String nombre, String contrasena) {
    var ids = em.createQuery("select u.idUsuario from Usuario u where lower(u.username)=:nombre", Long.class)
        .setParameter("nombre", nombre.strip().toLowerCase(java.util.Locale.ROOT)).setMaxResults(2).getResultList();
    var id = ids.size() == 1 ? ids.getFirst() : null;
    var u = id == null ? null : usuarios.bloquear(id).orElse(null);
    var c = u == null ? null : credencial(u.getIdUsuario());
    var fecha = ahora();
    if (c == null || c.getValorHash() == null || u.getEstadoUsuario() != EstadoUsuario.ACTIVO
        || (c.getFechaBloqueo() != null && c.getFechaBloqueo().isAfter(fecha))) {
      coincide(contrasena, HASH_SIMULADO);
      throw invalida();
    }
    if (c.getFechaBloqueo() != null) {
      c.setFechaBloqueo(null);
      c.setIntentosFallidos(0);
    }
    if (!coincide(contrasena, c.getValorHash())) {
      c.setIntentosFallidos(Math.min(5, c.getIntentosFallidos() + 1));
      if (c.getIntentosFallidos() == 5) c.setFechaBloqueo(fecha.plusMinutes(15));
      throw invalida();
    }
    if (vencida(c, fecha)) throw invalida();
    c.setIntentosFallidos(0);
    if (!temporal(c)) asegurarTurnoAbierto(u);
    return principal(c);
  }

  @Transactional(readOnly = true)
  public Cuenta vigente(Principal principal) {
    var c = credencial(principal.idUsuario());
    if (c == null || c.getValorHash() == null || c.getUsuario().getEstadoUsuario() != EstadoUsuario.ACTIVO
        || !Objects.equals(principal.firmaCredencial(), hash(c.getValorHash())) || vencida(c, ahora())) {
      throw invalida();
    }
    return new Cuenta(c.getUsuario(), temporal(c));
  }

  @Transactional
  public Principal cambiar(Principal principal, String actual, String nueva) {
    var usuario = usuarios.bloquear(principal.idUsuario()).orElseThrow(CredencialesService::invalida);
    vigente(principal);
    var c = credencial(principal.idUsuario());
    if (!coincide(actual, c.getValorHash())) throw invalida();
    validarContrasena(nueva);
    exigir(!coincide(nueva, c.getValorHash()), "Elige una contraseña diferente de la actual");
    boolean primeraActivacion = temporal(c);
    guardar(c, nueva, false);
    if (primeraActivacion) asegurarTurnoAbierto(usuario);
    return principal(c);
  }

  // El usuario ya está bloqueado en la transacción para serializar accesos simultáneos.
  private void asegurarTurnoAbierto(Usuario usuario) {
    var id = usuario.getIdUsuario();
    if (!accesos.perfil(id).operaCaja() ||
        !turnos.findByUsuarioIdUsuarioAndEstadoSesionAndFechaCierreIsNullOrderByIdTurno(id, 1).isEmpty()) return;
    var turno = new TurnoCaja();
    turno.setUsuario(usuario);
    turno.setEstadoSesion(1);
    turno.setFechaApertura(ahora());
    turnos.save(turno);
  }

  public Temporal emitir(Usuario usuario) {
    var datos = new byte[18];
    ALEATORIO.nextBytes(datos);
    var contrasena = Base64.getUrlEncoder().withoutPadding().encodeToString(datos);
    var c = preparar(usuario, contrasena);
    return new Temporal(contrasena, c.getFechaVencimiento());
  }

  public void invalidarSesiones(Usuario usuario) {
    var c = credencial(usuario.getIdUsuario());
    if (c != null) c.setValorHash(null);
  }

  @Transactional
  public void prepararGerente(String nombre, String contrasena) {
    var rol = accesos.buscarRol(AccesosService.GERENTE);
    if (rol == null) rol = accesos.crearRol(AccesosService.GERENTE);
    accesos.bloquearGerencia();
    var u = usuarios.findByUsernameIgnoreCase(nombre.strip()).orElseThrow(() ->
        new IllegalStateException("El usuario existente configurado para gerencia no se encontró"));
    if (accesos.perfil(u.getIdUsuario()).gerente()) return;
    long asignados = em.createQuery("select count(r) from UsuarioRol r where r.rol.idRol=:id", Long.class)
        .setParameter("id", rol.getIdRol()).getSingleResult();
    exigir(asignados == 0, "Ya existe un gerente configurado");
    exigir(u.getEstadoUsuario() == EstadoUsuario.ACTIVO, "El gerente inicial debe estar activo");
    validarContrasena(contrasena);
    preparar(u, contrasena);
    accesos.vincular(u.getIdUsuario(), rol.getIdRol());
  }

  private UsuarioCredencial preparar(Usuario usuario, String contrasena) {
    var c = credencial(usuario.getIdUsuario());
    if (c == null) {
      var tipo = em.createQuery("select t from TipoCredencial t where t.nombreCredencial=:nombre", TipoCredencial.class)
          .setParameter("nombre", TIPO).getResultList().stream().findFirst().orElse(null);
      if (tipo == null) {
        tipo = new TipoCredencial(null, TIPO);
        em.persist(tipo);
      }
      c = new UsuarioCredencial();
      c.setUsuario(usuario);
      c.setTipoCredencial(tipo);
      guardar(c, contrasena, true);
      em.persist(c);
    } else {
      guardar(c, contrasena, true);
    }
    return c;
  }

  private void guardar(UsuarioCredencial c, String contrasena, boolean temporal) {
    c.setValorHash(encoder.encode(contrasena));
    c.setCredencialTemporal(temporal ? 1 : 0);
    c.setFechaVencimiento(temporal ? ahora().plusHours(24) : null);
    c.setIntentosFallidos(0);
    c.setFechaBloqueo(null);
  }

  private UsuarioCredencial credencial(Long id) {
    return em.createQuery("""
        select c from UsuarioCredencial c join fetch c.usuario u
        where u.idUsuario=:id and c.tipoCredencial.nombreCredencial=:tipo
        """, UsuarioCredencial.class).setParameter("id", id).setParameter("tipo", TIPO)
        .getResultList().stream().findFirst().orElse(null);
  }

  private boolean coincide(String contrasena, String valor) {
    return contrasena.getBytes(StandardCharsets.UTF_8).length <= 72 && encoder.matches(contrasena, valor);
  }

  private void validarContrasena(String contrasena) {
    exigir(contrasena != null && !contrasena.isBlank() && contrasena.codePointCount(0, contrasena.length()) >= 15
        && contrasena.getBytes(StandardCharsets.UTF_8).length <= 72,
        "La contraseña debe tener al menos 15 caracteres y ocupar como máximo 72 bytes");
  }

  private static boolean temporal(UsuarioCredencial c) {
    return !Integer.valueOf(0).equals(c.getCredencialTemporal());
  }

  private static boolean vencida(UsuarioCredencial c, LocalDateTime fecha) {
    return temporal(c) && (c.getFechaVencimiento() == null || !fecha.isBefore(c.getFechaVencimiento()));
  }

  private Principal principal(UsuarioCredencial c) {
    return new Principal(c.getUsuario().getIdUsuario(), hash(c.getValorHash()));
  }

  private static BadCredentialsException invalida() {
    return new BadCredentialsException("Credenciales inválidas o vencidas");
  }
}
