package pe.com.rebolsysBackend.service.service;

import pe.com.rebolsysBackend.dto.*;

public interface VentaService {
  VentaResponseDTO registrar(VentaRequestDTO r);

  VentaResponseDTO registrarConfirmada(VentaRequestDTO r);

  VentaResponseDTO editar(Long id, VentaEditarDTO r);

  VentaResponseDTO obtener(Long id);

  PaginaResponseDTO<VentaResponseDTO> listar(int pagina, int tamanio);

  VentaResponseDTO confirmar(Long id, OperacionUsuarioDTO r);

  VentaResponseDTO anular(Long id, OperacionUsuarioDTO r);
}
