package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.exigir;
import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.hash;
import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.limpiar;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.com.rebolsysBackend.dto.SistemaDTO.*;
import pe.com.rebolsysBackend.entity.Hospedaje;
import pe.com.rebolsysBackend.exception.RecursoNoEncontradoException;
import pe.com.rebolsysBackend.repository.SistemaRepository;

@Service
@Transactional(readOnly = true)
public class HospedajeService {
  public static final String RUTA_LOGOS = "/api/v1/configuraciones/marca/logo/";
  private final SistemaRepository datos;
  private final AccesosService accesos;
  private final Path directorio;

  public HospedajeService(SistemaRepository datos, AccesosService accesos,
      @Value("${app.hospedaje.directorio-logotipos:.local/logotipos}") String directorio) {
    this.datos = datos;
    this.accesos = accesos;
    this.directorio = Path.of(directorio).toAbsolutePath().normalize();
  }

  public Marca marca() {
    var hospedaje = datos.hospedaje();
    return hospedaje == null ? new Marca(null, null, null, null) : respuesta(hospedaje);
  }

  @Transactional
  public Marca guardar(DatosHospedaje solicitud) {
    accesos.bloquearGerencia();
    var hospedaje = datos.hospedaje();
    boolean nuevo = hospedaje == null;
    if (nuevo) hospedaje = new Hospedaje();
    hospedaje.setNombre(solicitud.nombre().strip());
    hospedaje.setRuc(solicitud.ruc());
    hospedaje.setDireccion(limpiar(solicitud.direccion()));
    if (nuevo) datos.registrar(hospedaje);
    return respuesta(hospedaje);
  }

  @Transactional
  public Marca guardarLogo(MultipartFile archivo) {
    exigir(!archivo.isEmpty() && archivo.getSize() <= 5L * 1024 * 1024, "Selecciona un logo JPEG o PNG de hasta 5 MB");
    accesos.bloquearGerencia();
    var hospedaje = datos.hospedaje();
    exigir(hospedaje != null, "Guarda los datos del hospedaje antes de subir el logo");
    try {
      byte[] contenido = archivo.getBytes();
      String extension = ImagenEvidencia.extensionImagen(contenido);
      String nombre = hash(contenido) + "." + extension;
      Path destino = directorio.resolve(nombre);
      if (!Files.isRegularFile(destino)) {
        Files.createDirectories(directorio);
        Path temporal = Files.createTempFile(directorio, "logo-", ".tmp");
        try {
          Files.write(temporal, contenido);
          Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
          Files.deleteIfExists(temporal);
        }
      }
      hospedaje.setLogoRuta(RUTA_LOGOS + nombre);
      return respuesta(hospedaje);
    } catch (IOException ex) {
      throw new UncheckedIOException("No se pudo guardar el logo del hospedaje", ex);
    }
  }

  public FileSystemResource logo(String nombre) {
    if (!nombre.matches("[a-f0-9]{64}\\.(jpg|png)") || !(RUTA_LOGOS + nombre).equals(marca().logoRuta())) {
      throw new RecursoNoEncontradoException("El logo no está disponible");
    }
    var archivo = new FileSystemResource(directorio.resolve(nombre));
    if (!archivo.isReadable()) throw new RecursoNoEncontradoException("El logo no está disponible");
    return archivo;
  }

  private Marca respuesta(Hospedaje h) {
    var logo = h.getLogoRuta();
    return new Marca(h.getNombre(), h.getRuc(), h.getDireccion(),
        logo != null && logo.startsWith(RUTA_LOGOS) ? logo : null);
  }
}
