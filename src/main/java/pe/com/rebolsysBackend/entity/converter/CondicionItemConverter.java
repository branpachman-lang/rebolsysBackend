package pe.com.rebolsysBackend.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pe.com.rebolsysBackend.enums.EstadoConservacion;

@Converter
public class CondicionItemConverter implements AttributeConverter<EstadoConservacion, String> {
  @Override
  public String convertToDatabaseColumn(EstadoConservacion estado) {
    if (estado == null) return null;
    return estado == EstadoConservacion.BUENO ? "OPTIMO" : estado.name();
  }

  @Override
  public EstadoConservacion convertToEntityAttribute(String valor) {
    if (valor == null) return null;
    return "OPTIMO".equals(valor) ? EstadoConservacion.BUENO : EstadoConservacion.valueOf(valor);
  }
}
