package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.exigir;
import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.hash;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import pe.com.rebolsysBackend.exception.RecursoNoEncontradoException;

@Slf4j
@Service
public class EvidenciaCajaService {
  private final Path directorio;

  record Archivo(String nombre, String extension, String sha, byte[] contenido) {}

  public EvidenciaCajaService(@Value("${app.caja.directorio-evidencias:.local/evidencias-caja}") String directorio) {
    this.directorio = Path.of(directorio).toAbsolutePath().normalize();
  }

  Archivo preparar(MultipartFile archivo) {
    if (archivo == null) return null;
    exigir(!archivo.isEmpty() && archivo.getSize() <= 5L * 1024 * 1024,
        "La evidencia debe tener un tamaño máximo de 5 MB");
    try {
      byte[] contenido = archivo.getBytes();
      String extension = ImagenEvidencia.extensionImagen(contenido);
      String nombre = Objects.toString(archivo.getOriginalFilename(), "").replace('\\', '/');
      nombre = nombre.substring(nombre.lastIndexOf('/') + 1).replaceAll("[^\\p{L}\\p{N}._ -]", "_").strip();
      if (nombre.isBlank()) nombre = "evidencia." + extension;
      if (nombre.length() > 100) nombre = nombre.substring(0, 100 - extension.length() - 1) + "." + extension;
      return new Archivo(nombre, extension, hash(contenido), contenido);
    } catch (IOException ex) {
      throw new UncheckedIOException("No se pudo leer la evidencia de caja", ex);
    }
  }

  String guardar(Archivo archivo) {
    String nombre = UUID.randomUUID() + "." + archivo.extension();
    Path destino = directorio.resolve(nombre);
    try {
      Files.createDirectories(directorio);
      limpiarTemporales();
      Path temporal = Files.createTempFile(directorio, "caja-", ".tmp");
      try {
        Files.write(temporal, archivo.contenido());
        Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE);
      } finally {
        Files.deleteIfExists(temporal);
      }
    } catch (IOException ex) {
      throw new UncheckedIOException("No se pudo guardar la evidencia de caja", ex);
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status != STATUS_ROLLED_BACK) return;
        try {
          Files.deleteIfExists(destino);
        } catch (IOException ex) {
          log.warn("No se pudo retirar una evidencia de caja revertida", ex);
        }
      }
    });
    return "caja/" + nombre;
  }

  private void limpiarTemporales() {
    var vencimiento = Instant.now().minus(Duration.ofDays(1));
    try (var temporales = Files.newDirectoryStream(directorio, "caja-*.tmp")) {
      for (var temporal : temporales) {
        if (Files.isRegularFile(temporal, LinkOption.NOFOLLOW_LINKS)
            && Files.getLastModifiedTime(temporal, LinkOption.NOFOLLOW_LINKS).toInstant().isBefore(vencimiento)) {
          Files.deleteIfExists(temporal);
        }
      }
    } catch (IOException ex) {
      log.warn("No se pudieron retirar los archivos temporales de caja", ex);
    }
  }

  static boolean esRutaCaja(String ruta) {
    return ruta != null && ruta.matches("caja/[a-f0-9-]{36}\\.(jpg|png)");
  }

  FileSystemResource obtener(String ruta) {
    if (esRutaCaja(ruta)) {
      var archivo = directorio.resolve(ruta.substring("caja/".length()));
      if (Files.isRegularFile(archivo)) return new FileSystemResource(archivo);
    }
    throw new RecursoNoEncontradoException("La evidencia no existe");
  }
}
