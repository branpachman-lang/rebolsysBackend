package pe.com.rebolsysBackend.controller;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import pe.com.rebolsysBackend.dto.SesionActualDTO;
import pe.com.rebolsysBackend.service.impl.SesionOperacionService;
import pe.com.rebolsysBackend.service.impl.CredencialesService;
import pe.com.rebolsysBackend.config.Acceso;

@RestController
@RequestMapping("/api/v1/sesion")
@RequiredArgsConstructor
@Acceso(Acceso.SESION)
public class SesionOperacionController {
  private final SesionOperacionService sesion;
  private final CredencialesService credenciales;
  private final HttpSessionSecurityContextRepository sesiones;
  @Value("${app.sesion-prueba.habilitada:false}") private boolean prueba;

  public record Iniciar(@NotBlank @Size(max = 50) String usuario, @NotBlank @Size(max = 128) String contrasena) {
    @Override public String toString() { return "Solicitud de inicio de sesión"; }
  }
  public record Cambiar(@NotBlank @Size(max = 128) String actual, @NotBlank @Size(max = 128) String nueva) {
    @Override public String toString() { return "Solicitud de cambio de contraseña"; }
  }
  public record Proteccion(String token, String cabecera) {}

  @GetMapping("/csrf")
  @Acceso(Acceso.PUBLICO)
  public Proteccion csrf(CsrfToken token, HttpServletResponse res) {
    res.setHeader("Cache-Control", "no-store");
    return token == null ? new Proteccion(null, null) : new Proteccion(token.getToken(), token.getHeaderName());
  }

  @PostMapping("/iniciar")
  @Acceso(Acceso.PUBLICO)
  @Operation(operationId = "iniciarSesion")
  public SesionActualDTO iniciar(@Valid @RequestBody Iniciar datos, HttpServletRequest req, HttpServletResponse res) {
    if (prueba) throw new AuthenticationCredentialsNotFoundException("Desactiva la sesión de pruebas para iniciar sesión");
    autenticar(credenciales.iniciar(datos.usuario(), datos.contrasena()), req, res);
    return sesion.actual();
  }

  @GetMapping("/actual")
  @Operation(summary="Consultar el usuario, turno y permisos de la sesión")
  public SesionActualDTO actual(){return sesion.actual();}

  @PostMapping("/contrasena")
  public SesionActualDTO cambiar(@Valid @RequestBody Cambiar datos, HttpServletRequest req, HttpServletResponse res) {
    var autenticacion = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacion == null || !(autenticacion.getPrincipal() instanceof CredencialesService.Principal principal)) {
      throw new AuthenticationCredentialsNotFoundException("Inicia sesión para continuar");
    }
    autenticar(credenciales.cambiar(principal, datos.actual(), datos.nueva()), req, res);
    req.removeAttribute(SesionOperacionService.ATRIBUTO);
    return sesion.actual();
  }

  @PostMapping("/cerrar")
  @Acceso(Acceso.CIERRE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cerrar(HttpServletRequest req, HttpServletResponse res) {
    if (req.getSession(false) != null) req.getSession(false).invalidate();
    SecurityContextHolder.clearContext();
    res.setHeader("Cache-Control", "no-store");
  }

  private void autenticar(CredencialesService.Principal principal, HttpServletRequest req, HttpServletResponse res) {
    if (req.getSession(false) != null) req.getSession(false).invalidate();
    var contexto = SecurityContextHolder.createEmptyContext();
    contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, java.util.List.of()));
    SecurityContextHolder.setContext(contexto);
    sesiones.saveContext(contexto, req, res);
    res.setHeader("Cache-Control", "no-store");
  }
}
