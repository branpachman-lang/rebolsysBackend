package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.exigir;
import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.hash;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pe.com.rebolsysBackend.dto.LimpiezaDTO.Evidencia;
import pe.com.rebolsysBackend.exception.RecursoNoEncontradoException;

@Service
public class EvidenciaLimpiezaService {
  public static final String RUTA = "/api/v1/limpieza/evidencias";
  private static final long MAX_BYTES = 5L * 1024 * 1024;

  private final Path directorio;
  private final SesionOperacionService sesion;

  public EvidenciaLimpiezaService(@Value("${app.limpieza.directorio-evidencias:.local/evidencias-limpieza}") String directorio,
      SesionOperacionService sesion) {
    this.directorio = Path.of(directorio).toAbsolutePath().normalize();
    this.sesion = sesion;
  }

  public Evidencia guardar(MultipartFile archivo) {
    sesion.actual();
    exigir(!archivo.isEmpty() && archivo.getSize() <= MAX_BYTES, "La foto debe tener un tamaño máximo de 5 MB");
    try {
      byte[] contenido = archivo.getBytes();
      String extension = ImagenEvidencia.extensionImagen(contenido);
      String sha = hash(contenido);
      String nombre = sha + "." + extension;
      Path destino = directorio.resolve(nombre);
      if (!Files.isRegularFile(destino)) {
        Files.createDirectories(directorio);
        Path temporal = Files.createTempFile(directorio, "foto-", ".tmp");
        try {
          Files.write(temporal, contenido);
          Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
          Files.deleteIfExists(temporal);
        }
      }
      return new Evidencia(RUTA + "/" + nombre, sha);
    } catch (IOException ex) {
      throw new UncheckedIOException("No se pudo guardar la evidencia de limpieza", ex);
    }
  }

  public FileSystemResource obtener(String nombre) {
    sesion.actual();
    if (!nombre.matches("[a-f0-9]{64}\\.(png|jpg)") || !Files.isRegularFile(directorio.resolve(nombre))) {
      throw new RecursoNoEncontradoException("La evidencia no existe");
    }
    return new FileSystemResource(directorio.resolve(nombre));
  }

  public void verificar(Evidencia evidencia) {
    String sha = evidencia.evidenciaSha256().toLowerCase(Locale.ROOT);
    String ruta = evidencia.evidenciaRuta().strip();
    exigir((ruta.equals(RUTA + "/" + sha + ".png") && Files.isRegularFile(directorio.resolve(sha + ".png")))
        || (ruta.equals(RUTA + "/" + sha + ".jpg") && Files.isRegularFile(directorio.resolve(sha + ".jpg"))),
        "Adjunta una evidencia fotográfica cargada en el módulo de limpieza");
  }

}
