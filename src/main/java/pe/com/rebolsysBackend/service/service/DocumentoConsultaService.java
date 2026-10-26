package pe.com.rebolsysBackend.service.service;

import pe.com.rebolsysBackend.dto.DocumentoConsultaResponseDTO;

public interface DocumentoConsultaService {
    DocumentoConsultaResponseDTO consultarDni(String numero);
    DocumentoConsultaResponseDTO consultarRuc(String numero);
}
