package pe.com.rebolsysBackend.service.service;

import java.util.List;
import pe.com.rebolsysBackend.dto.PaginaResponseDTO;
import pe.com.rebolsysBackend.dto.ReservaVentaResponseDTO;
import pe.com.rebolsysBackend.dto.VentaRequestDTO;
import pe.com.rebolsysBackend.dto.VentaResponseDTO;
import pe.com.rebolsysBackend.dto.VentaActualizarRequestDTO;

public interface VentaService {
    VentaResponseDTO registrar(VentaRequestDTO request);
    VentaResponseDTO actualizar(Long id, VentaActualizarRequestDTO request);
    VentaResponseDTO buscar(Long id);
    PaginaResponseDTO<VentaResponseDTO> listar(int pagina, int tamanio);
    List<ReservaVentaResponseDTO> buscarReservas(String buscar);
}
