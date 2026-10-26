package pe.com.rebolsysBackend.service.service;

import pe.com.rebolsysBackend.dto.CancelacionReservaRequestDTO;
import pe.com.rebolsysBackend.dto.ComprobanteInternoResponseDTO;
import pe.com.rebolsysBackend.dto.PaginaResponseDTO;
import pe.com.rebolsysBackend.dto.PagoEfectivoRequestDTO;
import pe.com.rebolsysBackend.dto.ReservaRegistroRequestDTO;
import pe.com.rebolsysBackend.dto.ReservaResumenResponseDTO;
import pe.com.rebolsysBackend.service.generic.CrudService;

public interface ReservaService extends CrudService<ReservaRegistroRequestDTO, ReservaResumenResponseDTO, String> {
    PaginaResponseDTO<ReservaResumenResponseDTO> listar(int pagina, int tamanio, String buscar, String estado);

    ReservaResumenResponseDTO pagar(String referencia, PagoEfectivoRequestDTO request);

    ReservaResumenResponseDTO cancelar(String referencia, CancelacionReservaRequestDTO request);

    ComprobanteInternoResponseDTO comprobante(String referencia);
    ComprobanteInternoResponseDTO comprobante(String referencia, Long idComprobante);
}
