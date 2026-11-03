package pe.com.rebolsysBackend.service.service;

import pe.com.rebolsysBackend.dto.*;

public interface ReservaService {
  ReservaRespuestaDTO registrar(ReservaRegistroRequestDTO r);

  ReservaRespuestaDTO obtener(Long id);

  PaginaResponseDTO<ReservaRespuestaDTO> listar(int pagina, int tamanio);

  ReservaDetalleRespuestaDTO cambiarEstado(Long id, String accion, OperacionUsuarioDTO r);

  ReservaDetalleRespuestaDTO guardarOcupantes(Long id, ReservaOcupantesGuardarDTO r);
}
