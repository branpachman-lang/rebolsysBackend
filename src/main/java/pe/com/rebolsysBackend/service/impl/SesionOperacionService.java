package pe.com.rebolsysBackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pe.com.rebolsysBackend.dto.SesionActualDTO;
import pe.com.rebolsysBackend.enums.EstadoUsuario;
import pe.com.rebolsysBackend.repository.*;
import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.exigir;

@Service
@RequiredArgsConstructor
public class SesionOperacionService {
  public static final String ATRIBUTO = "rebolsys.sesion.actual";
  private final UsuarioRepository usuarios;
  private final TurnoCajaRepository turnos;
  private final FormaPagoRepository pagos;
  private final CredencialesService credenciales;
  private final AccesosService accesos;
  @Value("${app.sesion-prueba.habilitada:false}")
  private boolean habilitada;
  @Value("${app.sesion-prueba.usuario:operacion_local}")
  private String username;

  @Transactional(readOnly = true)
  public SesionActualDTO actual() {
    var atributos = RequestContextHolder.getRequestAttributes();
    var request = atributos instanceof ServletRequestAttributes a ? a.getRequest() : null;
    if (request != null && request.getAttribute(ATRIBUTO) instanceof SesionActualDTO actual) return actual;
    var autenticacion = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacion != null && autenticacion.getPrincipal() instanceof CredencialesService.Principal principal) {
      var cuenta = credenciales.vigente(principal);
      var u = cuenta.usuario();
      var perfil = cuenta.requiereCambio() ? null : accesos.perfil(u.getIdUsuario());
      var actual = cuenta.requiereCambio()
          ? new SesionActualDTO(u.getIdUsuario(), u.getUsername(), "ACTIVA", null, null, true, false,
              java.util.List.of(), java.util.List.of())
          : new SesionActualDTO(u.getIdUsuario(), u.getUsername(), "ACTIVA", turno(u.getIdUsuario()), efectivo(),
              false, perfil.gerente(), perfil.roles(), perfil.permisos());
      if (request != null) request.setAttribute(ATRIBUTO, actual);
      return actual;
    }
    if (!habilitada) throw new AuthenticationCredentialsNotFoundException("Inicia sesión para continuar");
    var usuario = usuarios.findByUsername(username).orElseThrow(() -> new pe.com.rebolsysBackend.exception.ReglaNegocioException("El usuario de la sesión de prueba no existe"));
    exigir(usuario.getEstadoUsuario() == EstadoUsuario.ACTIVO, "El usuario de la sesión debe estar activo");
    var actual = new SesionActualDTO(usuario.getIdUsuario(), usuario.getUsername(), "PRUEBA",
            turno(usuario.getIdUsuario()), efectivo());
    if (request != null) request.setAttribute(ATRIBUTO, actual);
    return actual;
  }

  public static SesionActualDTO enPeticion() {
    var atributos = RequestContextHolder.getRequestAttributes();
    if (atributos instanceof ServletRequestAttributes req
        && req.getRequest().getAttribute(ATRIBUTO) instanceof SesionActualDTO actual) return actual;
    return null;
  }

  public static void exigirPermisoEnPeticion(String permiso) {
    var actual = enPeticion();
    if (actual != null && !actual.tiene(permiso)) {
      throw new AccessDeniedException("Tu usuario no tiene acceso a esta función");
    }
  }

  private Long turno(Long idUsuario) {
    var abiertos = turnos.findByUsuarioIdUsuarioAndEstadoSesionAndFechaCierreIsNullOrderByIdTurno(idUsuario, 1);
    return abiertos.size() == 1 ? abiertos.getFirst().getIdTurno() : null;
  }

  private Long efectivo() {
    return pagos.findAll().stream().filter(p -> "EFECTIVO".equalsIgnoreCase(p.getNombrePago().trim()))
        .findFirst().map(p -> p.getIdFormaPago()).orElse(null);
  }
}
