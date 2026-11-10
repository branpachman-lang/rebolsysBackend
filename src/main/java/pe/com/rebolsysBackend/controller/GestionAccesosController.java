package pe.com.rebolsysBackend.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.com.rebolsysBackend.config.Acceso;
import pe.com.rebolsysBackend.dto.SeguridadDTO.*;
import pe.com.rebolsysBackend.service.impl.*;

@RestController
@RequestMapping("/api/v1/seguridad")
@Acceso(Acceso.GERENTE)
@RequiredArgsConstructor
public class GestionAccesosController {
  private final GestionAccesosService gestion;
  private final AccesosService accesos;

  @GetMapping("/empleados")
  @Acceso("EMPLEADOS:VER")
  public List<EmpleadoVista> empleados() { return gestion.empleados(); }

  @PostMapping("/empleados")
  @Acceso("EMPLEADOS:CREAR")
  @ResponseStatus(HttpStatus.CREATED)
  public EmpleadoVista crearEmpleado(@Valid @RequestBody NuevoEmpleado datos) { return gestion.crearEmpleado(datos); }

  @PutMapping("/empleados/{id}")
  public EmpleadoVista editarEmpleado(@PathVariable @Positive Long id, @Valid @RequestBody EditarEmpleado datos) {
    return gestion.editarEmpleado(id, datos);
  }

  @PutMapping("/empleados/{id}/estado")
  public EmpleadoVista cambiarEstadoEmpleado(@PathVariable @Positive Long id, @Valid @RequestBody EstadoEmpleado datos) {
    return gestion.cambiarEstadoEmpleado(id, datos.activo());
  }

  @GetMapping("/usuarios")
  @Acceso("USUARIOS:VER")
  public List<UsuarioVista> usuarios() { return gestion.usuarios(); }

  @PostMapping("/usuarios")
  @ResponseStatus(HttpStatus.CREATED)
  public UsuarioCreado crearUsuario(@Valid @RequestBody NuevoUsuario datos, HttpServletResponse res) {
    res.setHeader("Cache-Control", "no-store");
    return gestion.crearUsuario(datos);
  }

  @PutMapping("/usuarios/{id}/accesos")
  public UsuarioVista cambiarAccesos(@PathVariable @Positive Long id, @Valid @RequestBody SeleccionAccesos datos) {
    return gestion.cambiarAccesos(id, datos);
  }

  @PutMapping("/usuarios/{id}")
  public UsuarioVista editarUsuario(@PathVariable @Positive Long id, @Valid @RequestBody EditarUsuario datos) {
    return gestion.editarUsuario(id, datos);
  }

  @PutMapping("/usuarios/{id}/estado")
  @Operation(operationId = "cambiarEstadoUsuario")
  public UsuarioVista cambiarEstado(@PathVariable @Positive Long id, @Valid @RequestBody CambiarEstado datos) {
    return gestion.cambiarEstado(id, datos);
  }

  @PostMapping("/usuarios/{id}/credencial-temporal")
  public CredencialesService.Temporal reemitir(@PathVariable @Positive Long id, HttpServletResponse res) {
    res.setHeader("Cache-Control", "no-store");
    return gestion.reemitir(id);
  }

  @GetMapping("/funcionalidades")
  public List<AccesosService.Funcion> funcionalidades() { return accesos.catalogo(); }

  @GetMapping("/roles")
  @Acceso("ROLES:VER")
  public List<RolVista> roles() { return gestion.roles(); }

  @PostMapping("/roles")
  @ResponseStatus(HttpStatus.CREATED)
  public RolVista crearRol(@Valid @RequestBody GuardarRol datos) { return gestion.guardarRol(null, datos); }

  @PutMapping("/roles/{id}")
  public RolVista editarRol(@PathVariable @Positive Long id, @Valid @RequestBody GuardarRol datos) {
    return gestion.guardarRol(id, datos);
  }

  @DeleteMapping("/roles/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminarRol(@PathVariable @Positive Long id) { gestion.eliminarRol(id); }
}
