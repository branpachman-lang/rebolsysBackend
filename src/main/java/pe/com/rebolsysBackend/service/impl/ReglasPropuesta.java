package pe.com.rebolsysBackend.service.impl;

import jakarta.validation.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.enums.*;
import pe.com.rebolsysBackend.exception.*;
import pe.com.rebolsysBackend.repository.*;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class ReglasPropuesta {
  private final UsuarioRepository usuarios;
  private final TipoMovimientoAlmacenRepository tipos;
  private final Validator validator;
  private final ObjectMapper mapper;

  public <T> T validar(T r) {
    var errores = validator.validate(r);
    if (!errores.isEmpty()) throw new ConstraintViolationException(errores);
    return r;
  }

  public Usuario usuario(Long id) {
    var u =
        usuarios
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe"));
    exigir(u.getEstadoUsuario() == EstadoUsuario.ACTIVO, "El usuario debe estar activo");
    return u;
  }

  public Usuario bloquearUsuario(Long id) {
    var u =
        usuarios
            .bloquear(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe"));
    exigir(u.getEstadoUsuario() == EstadoUsuario.ACTIVO, "El usuario debe estar activo");
    return u;
  }

  public TipoMovimientoAlmacen tipo(String codigo, int signo) {
    var t =
        tipos
            .findByCodigo(codigo)
            .orElseThrow(
                () ->
                    new ReglaNegocioException(
                        "Falta configurar el movimiento de almacén: " + codigo));
    exigir(t.getSigno() == signo, "El signo del movimiento no es válido");
    return t;
  }

  public String firma(Object objeto) {
    return hash(mapper.writeValueAsString(objeto));
  }

  public static String hash(String texto) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public static void exigir(boolean condicion, String mensaje) {
    if (!condicion) throw new ReglaNegocioException(mensaje);
  }

  public static String limpiar(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }

  public static String patronBusqueda(String valor) {
    return "%" + (valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT))
        .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
  }

  public static java.time.LocalDateTime ahora() {
    return java.time.LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
  }
}
