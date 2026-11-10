package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.exigir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import pe.com.rebolsysBackend.exception.ReglaNegocioException;

final class ImagenEvidencia {
  private ImagenEvidencia() {}

  static String extensionImagen(byte[] contenido) {
    try (var entrada = new MemoryCacheImageInputStream(new ByteArrayInputStream(contenido))) {
      var lectores = ImageIO.getImageReaders(entrada);
      exigir(lectores.hasNext(), "La evidencia debe ser una imagen JPEG o PNG");
      var lector = lectores.next();
      try {
        String formato = lector.getFormatName().toLowerCase(Locale.ROOT);
        exigir(formato.equals("jpeg") || formato.equals("png"), "La evidencia debe ser una imagen JPEG o PNG");
        lector.setInput(entrada);
        exigir((long) lector.getWidth(0) * lector.getHeight(0) <= 20_000_000, "La foto supera 20 megapíxeles");
        lector.read(0);
        return formato.equals("jpeg") ? "jpg" : formato;
      } finally {
        lector.dispose();
      }
    } catch (IOException ex) {
      throw new ReglaNegocioException("La evidencia fotográfica está dañada o incompleta");
    }
  }
}
