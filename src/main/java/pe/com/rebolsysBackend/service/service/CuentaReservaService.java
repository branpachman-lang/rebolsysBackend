package pe.com.rebolsysBackend.service.service;

import java.util.List;
import pe.com.rebolsysBackend.dto.*;

public interface CuentaReservaService {
  CuentaReservaDTO cuenta(Long idReserva);

  CuentaReservaDTO pagar(Long idReserva, ReservaPagoRegistroDTO request);

  CuentaDocumentoDTO emitir(Long idReserva, ReservaComprobanteRegistroDTO request);

  ConsumoReservaRespuestaDTO consumir(Long idReserva, VentaRequestDTO request);

  List<ConsumoReservaRespuestaDTO> consumos(Long idReserva);
}
