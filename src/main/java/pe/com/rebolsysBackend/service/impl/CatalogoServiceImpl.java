package pe.com.rebolsysBackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.CatalogoItemResponseDTO;
import pe.com.rebolsysBackend.dto.CatalogosResponseDTO;
import pe.com.rebolsysBackend.repository.FormaPagoRepository;
import pe.com.rebolsysBackend.repository.TipoDocumentoRepository;
import pe.com.rebolsysBackend.repository.TipoOcupanteRepository;
import pe.com.rebolsysBackend.service.service.CatalogoService;

@Service
@RequiredArgsConstructor
public class CatalogoServiceImpl implements CatalogoService {

    private final TipoDocumentoRepository documentos;
    private final TipoOcupanteRepository ocupantes;
    private final FormaPagoRepository pagos;

    @Override
    @Transactional(readOnly = true)
    public CatalogosResponseDTO catalogos() {
        return new CatalogosResponseDTO(
            documentos
                .findByNombreDocumentoNotOrderByIdTipoDocumento("INTERNO")
                .stream()
                .map(tipo ->
                    new CatalogoItemResponseDTO(
                        tipo.getIdTipoDocumento(),
                        tipo.getNombreDocumento(),
                        tipo.getCodigoSunat()
                    )
                )
                .toList(),
            ocupantes
                .findAll()
                .stream()
                .map(tipo -> new CatalogoItemResponseDTO(tipo.getIdTipoOcupante(), tipo.getNombreTipo()))
                .toList(),
            pagos
                .findByNombrePagoIgnoreCase("EFECTIVO")
                .stream()
                .map(tipo -> new CatalogoItemResponseDTO(tipo.getIdFormaPago(), tipo.getNombrePago()))
                .toList()
        );
    }
}
