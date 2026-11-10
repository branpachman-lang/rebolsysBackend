package pe.com.rebolsysBackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import pe.com.rebolsysBackend.enums.EstadoUsuario;
import pe.com.rebolsysBackend.service.impl.CredencialesService.Temporal;

public final class SeguridadDTO {
  private SeguridadDTO() {}
  private static final String FORMATO_USUARIO = "[a-zA-Z0-9][a-zA-Z0-9._@+-]{2,49}";
  private static final String MENSAJE_USUARIO = "El usuario debe tener entre 3 y 50 caracteres y usar letras, números o . _ @ + -";

  public record NuevoEmpleado(@NotNull @Positive Long idTipoDocumento, @NotBlank @Size(max = 50) String numeroDocumento,
      @NotBlank @Size(max = 100) String nombres, @NotBlank @Size(max = 100) String apellidos,
      @NotBlank @Email @Size(max = 100) String correo, @Size(max = 15) String telefono,
      @Past LocalDate fechaNacimiento, @NotNull @PastOrPresent LocalDate fechaContratacion) {}

  public record EmpleadoVista(Long idEmpleado, Long idTipoDocumento, String numeroDocumento, String nombres,
      String apellidos, String correo, String telefono, LocalDate fechaNacimiento, LocalDate fechaContratacion,
      LocalDate fechaCese, boolean tieneUsuario, boolean protegido) {}

  public record EditarEmpleado(@NotBlank @Size(max = 100) String nombres, @NotBlank @Size(max = 100) String apellidos,
      @Email @Size(max = 100) String correo, @Size(max = 15) String telefono,
      @Past LocalDate fechaNacimiento, @NotNull @PastOrPresent LocalDate fechaContratacion) {}

  public record EstadoEmpleado(@NotNull Boolean activo) {}

  public record SeleccionAccesos(@NotNull @Size(max = 50) Set<@NotNull @Positive Long> roles,
      @NotNull @Size(max = 250) Set<@NotBlank @Size(max = 151) String> permisos) {}

  public record NuevoUsuario(@NotNull @Positive Long idEmpleado,
      @NotBlank @Pattern(regexp = FORMATO_USUARIO, message = MENSAJE_USUARIO) String usuario,
      @NotNull @Valid SeleccionAccesos accesos) {}

  public record EditarUsuario(@NotBlank @Pattern(regexp = FORMATO_USUARIO, message = MENSAJE_USUARIO) String usuario,
      @NotNull @Valid SeleccionAccesos accesos) {}

  public record UsuarioVista(Long idUsuario, Long idEmpleado, String usuario, EstadoUsuario estado, List<Long> roles,
      List<String> nombresRoles, List<String> permisosIndividuales, List<String> permisosEfectivos, boolean gerente) {}

  public record UsuarioCreado(UsuarioVista cuenta, Temporal credencial) {}

  public record CambiarEstado(@NotNull EstadoUsuario estado) {}

  public record GuardarRol(@NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{2,30}") String nombre,
      @NotNull @Size(max = 250) Set<@NotBlank @Size(max = 151) String> permisos) {}

  public record RolVista(Long idRol, String nombre, List<String> permisos, boolean protegido, long usuariosAsignados) {}
}
