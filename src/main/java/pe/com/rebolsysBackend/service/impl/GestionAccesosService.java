package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.*;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.SeguridadDTO.*;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.enums.EstadoUsuario;
import pe.com.rebolsysBackend.exception.RecursoNoEncontradoException;
import pe.com.rebolsysBackend.repository.UsuarioRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GestionAccesosService {
  private final EntityManager em;
  private final UsuarioRepository usuarios;
  private final AccesosService accesos;
  private final CredencialesService credenciales;

  public List<EmpleadoVista> empleados() {
    return em.createQuery("select e from Empleado e order by e.idEmpleado desc", Empleado.class).getResultList()
        .stream().map(this::empleadoVista).toList();
  }

  @Transactional
  public EmpleadoVista crearEmpleado(NuevoEmpleado datos) {
    var tipo = em.find(TipoDocumento.class, datos.idTipoDocumento());
    if (tipo == null) throw new RecursoNoEncontradoException("El tipo de documento no existe");
    var documento = datos.numeroDocumento().strip();
    var existente = em.createQuery("""
        select p from Perfil p where p.tipoDocumento=:tipo and p.numeroDocumento=:numero
        """, Perfil.class).setParameter("tipo", tipo).setParameter("numero", documento)
        .getResultList().stream().findFirst().orElse(null);
    var correo = datos.correo().strip().toLowerCase(Locale.ROOT);
    PersonaNatural persona;
    if (existente == null) {
      var perfil = new Perfil(null, tipo, documento, correo, limpiar(datos.telefono()));
      em.persist(perfil);
      persona = new PersonaNatural(null, perfil, datos.nombres().strip(), datos.apellidos().strip(), datos.fechaNacimiento());
      em.persist(persona);
    } else {
      persona = em.find(PersonaNatural.class, existente.getIdPerfil());
      exigir(persona != null && persona.getNombres().equalsIgnoreCase(datos.nombres().strip())
          && persona.getApellidos().equalsIgnoreCase(datos.apellidos().strip()),
          "El documento pertenece a un perfil con datos distintos");
      exigir(existente.getCorreo() == null || existente.getCorreo().equalsIgnoreCase(correo),
          "El correo no coincide con el perfil existente");
      exigir(em.createQuery("select count(e) from Empleado e where e.personaNatural=:persona", Long.class)
          .setParameter("persona", persona).getSingleResult() == 0, "El empleado ya está registrado");
      existente.setCorreo(correo);
      if (existente.getTelefono() == null) existente.setTelefono(limpiar(datos.telefono()));
      if (persona.getFechaNacimiento() == null) persona.setFechaNacimiento(datos.fechaNacimiento());
    }
    var empleado = new Empleado(null, persona, datos.fechaContratacion(), null);
    em.persist(empleado);
    return empleadoVista(empleado);
  }

  @Transactional
  public EmpleadoVista editarEmpleado(Long id, EditarEmpleado datos) {
    accesos.bloquearGerencia();
    var empleado = buscarEmpleado(id);
    exigir(empleado.getFechaCese() == null || !datos.fechaContratacion().isAfter(empleado.getFechaCese()),
        "La contratación debe ser anterior o igual a la fecha de cese");
    var persona = empleado.getPersonaNatural();
    persona.setNombres(datos.nombres().strip());
    persona.setApellidos(datos.apellidos().strip());
    persona.setFechaNacimiento(datos.fechaNacimiento());
    var correo = limpiar(datos.correo());
    persona.getPerfil().setCorreo(correo == null ? null : correo.toLowerCase(Locale.ROOT));
    persona.getPerfil().setTelefono(limpiar(datos.telefono()));
    empleado.setFechaContratacion(datos.fechaContratacion());
    return empleadoVista(empleado);
  }

  @Transactional
  public EmpleadoVista cambiarEstadoEmpleado(Long id, boolean activo) {
    accesos.bloquearGerencia();
    var empleado = buscarEmpleado(id);
    if (activo) {
      empleado.setFechaCese(null);
    } else if (empleado.getFechaCese() == null) {
      var hoy = ahora().toLocalDate();
      exigir(!hoy.isBefore(empleado.getFechaContratacion()), "El cese debe ser posterior o igual a la contratación");
      usuarios.findByEmpleadoIdEmpleado(id).ifPresent(cuenta -> {
        var usuario = usuarioAdministrable(cuenta.getIdUsuario());
        usuario.setEstadoUsuario(EstadoUsuario.INACTIVO);
        credenciales.invalidarSesiones(usuario);
      });
      empleado.setFechaCese(hoy);
    }
    return empleadoVista(empleado);
  }

  public List<UsuarioVista> usuarios() {
    return usuarios.findAll(org.springframework.data.domain.Sort.by("username")).stream().map(this::usuarioVista).toList();
  }

  @Transactional
  public UsuarioCreado crearUsuario(NuevoUsuario datos) {
    accesos.bloquearGerencia();
    var empleado = em.find(Empleado.class, datos.idEmpleado());
    if (empleado == null) throw new RecursoNoEncontradoException("El empleado no existe");
    exigir(empleado.getFechaCese() == null, "El empleado ya cesó");
    exigir(!tieneUsuario(empleado.getIdEmpleado()), "El empleado ya tiene un usuario");
    var nombre = datos.usuario().toLowerCase(Locale.ROOT);
    exigir(!usuarios.existsByUsernameIgnoreCase(nombre), "El nombre de usuario ya existe");
    exigir(!AccesosService.GERENTE.equalsIgnoreCase(nombre), "El nombre de usuario está reservado");
    var usuario = usuarios.save(new Usuario(null, empleado, nombre, EstadoUsuario.ACTIVO));
    accesos.asignar(usuario.getIdUsuario(), datos.accesos().roles(), datos.accesos().permisos());
    var temporal = credenciales.emitir(usuario);
    return new UsuarioCreado(usuarioVista(usuario), temporal);
  }

  @Transactional
  public UsuarioVista cambiarAccesos(Long id, SeleccionAccesos datos) {
    accesos.bloquearGerencia();
    var usuario = usuarioAdministrable(id);
    accesos.asignar(id, datos.roles(), datos.permisos());
    return usuarioVista(usuario);
  }

  @Transactional
  public UsuarioVista editarUsuario(Long id, EditarUsuario datos) {
    accesos.bloquearGerencia();
    var usuario = usuarioAdministrable(id);
    var nombre = datos.usuario().toLowerCase(Locale.ROOT);
    exigir(!AccesosService.GERENTE.equalsIgnoreCase(nombre), "El nombre de usuario está reservado");
    exigir(!usuarios.existsByUsernameIgnoreCaseAndIdUsuarioNot(nombre, id), "El nombre de usuario ya existe");
    usuario.setUsername(nombre);
    accesos.asignar(id, datos.accesos().roles(), datos.accesos().permisos());
    return usuarioVista(usuario);
  }

  @Transactional
  public UsuarioVista cambiarEstado(Long id, CambiarEstado datos) {
    var usuario = usuarioAdministrable(id);
    exigir(datos.estado() != EstadoUsuario.ACTIVO || usuario.getEmpleado().getFechaCese() == null,
        "Reactiva al empleado antes de reactivar su usuario");
    usuario.setEstadoUsuario(datos.estado());
    if (datos.estado() != EstadoUsuario.ACTIVO) credenciales.invalidarSesiones(usuario);
    return usuarioVista(usuario);
  }

  @Transactional
  public CredencialesService.Temporal reemitir(Long id) {
    accesos.bloquearGerencia();
    var usuario = usuarioAdministrable(id);
    exigir(usuario.getEstadoUsuario() == EstadoUsuario.ACTIVO, "La cuenta debe estar activa");
    return credenciales.emitir(usuario);
  }

  public List<RolVista> roles() {
    return accesos.roles().stream().map(this::rolVista).toList();
  }

  @Transactional
  public RolVista guardarRol(Long id, GuardarRol datos) {
    accesos.bloquearGerencia();
    var nombre = datos.nombre().toUpperCase(Locale.ROOT);
    exigir(!nombre.equals(AccesosService.GERENTE), "El rol GERENTE está reservado");
    var existente = accesos.buscarRol(nombre);
    exigir(existente == null || existente.getIdRol().equals(id), "Ya existe un rol con ese nombre");
    var rol = id == null ? accesos.crearRol(nombre) : rolAdministrable(id);
    rol.setNombreRol(nombre);
    accesos.permisos(rol.getIdRol(), datos.permisos());
    return rolVista(rol);
  }

  @Transactional
  public void eliminarRol(Long id) {
    accesos.bloquearGerencia();
    accesos.eliminarRol(rolAdministrable(id));
  }

  private Rol rolAdministrable(Long id) {
    var rol = em.find(Rol.class, id);
    if (rol == null) throw new RecursoNoEncontradoException("El rol no existe");
    exigir(!rol.getNombreRol().equals(AccesosService.GERENTE) && !rol.getNombreRol().startsWith("@"),
        "El rol está protegido");
    return rol;
  }

  private Empleado buscarEmpleado(Long id) {
    var empleado = em.find(Empleado.class, id);
    if (empleado == null) throw new RecursoNoEncontradoException("El empleado no existe");
    return empleado;
  }

  private Usuario usuarioAdministrable(Long id) {
    var usuario = usuarios.bloquear(id).orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe"));
    exigir(!accesos.esGerente(id), "La cuenta de gerencia está protegida");
    return usuario;
  }

  private EmpleadoVista empleadoVista(Empleado e) {
    var p = e.getPersonaNatural();
    var perfil = p.getPerfil();
    var cuenta = usuarios.findByEmpleadoIdEmpleado(e.getIdEmpleado());
    return new EmpleadoVista(e.getIdEmpleado(), perfil.getTipoDocumento().getIdTipoDocumento(),
        perfil.getNumeroDocumento(), p.getNombres(), p.getApellidos(), perfil.getCorreo(), perfil.getTelefono(),
        p.getFechaNacimiento(), e.getFechaContratacion(), e.getFechaCese(), cuenta.isPresent(),
        cuenta.map(u -> accesos.esGerente(u.getIdUsuario())).orElse(false));
  }

  private boolean tieneUsuario(Long empleado) {
    return usuarios.existsByEmpleadoIdEmpleado(empleado);
  }

  private UsuarioVista usuarioVista(Usuario u) {
    var perfil = accesos.perfil(u.getIdUsuario());
    return new UsuarioVista(u.getIdUsuario(), u.getEmpleado().getIdEmpleado(), u.getUsername(), u.getEstadoUsuario(),
        perfil.idsRoles(), perfil.roles(), perfil.individuales(), perfil.permisos(), perfil.gerente());
  }

  private RolVista rolVista(Rol r) {
    return new RolVista(r.getIdRol(), r.getNombreRol(), accesos.permisosRol(r.getIdRol()),
        r.getNombreRol().equals(AccesosService.GERENTE), accesos.usuariosAsignados(r.getIdRol()));
  }
}
