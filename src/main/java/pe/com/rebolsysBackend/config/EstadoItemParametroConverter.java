package pe.com.rebolsysBackend.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import pe.com.rebolsysBackend.enums.EstadoItem;

/** Admite los mismos valores de estado en filtros HTTP y cuerpos JSON. */
@Component
public class EstadoItemParametroConverter implements Converter<String, EstadoItem> {
  @Override
  public EstadoItem convert(String valor) {
    return EstadoItem.desdeValor(valor);
  }
}
