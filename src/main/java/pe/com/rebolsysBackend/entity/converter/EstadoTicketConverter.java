package pe.com.rebolsysBackend.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import pe.com.rebolsysBackend.enums.EstadoTicket;

@Converter
public class EstadoTicketConverter implements AttributeConverter<EstadoTicket, String> {
  @Override
  public String convertToDatabaseColumn(EstadoTicket estado) {
    if (estado == null) return null;
    return estado == EstadoTicket.RESUELTO ? "EMITIDO" : estado == EstadoTicket.CANCELADO ? "INVALIDADO" : estado.name();
  }

  @Override
  public EstadoTicket convertToEntityAttribute(String valor) {
    if (valor == null) return null;
    return "EMITIDO".equals(valor) ? EstadoTicket.RESUELTO : "INVALIDADO".equals(valor) ? EstadoTicket.CANCELADO : EstadoTicket.valueOf(valor);
  }
}
