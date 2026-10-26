package pe.com.rebolsysBackend.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pe.com.rebolsysBackend.enums.EstadoItem;

@Converter
public class EstadoItemConverter implements AttributeConverter<EstadoItem, String> {

    @Override
    public String convertToDatabaseColumn(EstadoItem estado) {
        return estado == null ? null : estado.getValor();
    }

    @Override
    public EstadoItem convertToEntityAttribute(String valor) {
        return valor == null ? null : EstadoItem.desdeValor(valor);
    }
}
