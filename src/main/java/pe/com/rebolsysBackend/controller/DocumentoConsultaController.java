package pe.com.rebolsysBackend.controller;

import pe.com.rebolsysBackend.config.Acceso;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.rebolsysBackend.dto.DocumentoConsultaResponseDTO;
import pe.com.rebolsysBackend.service.service.DocumentoConsultaService;

@RestController
@RequestMapping("/api/v1/documentos")
public class DocumentoConsultaController {

    private final DocumentoConsultaService documentoConsultaService;

    public DocumentoConsultaController(DocumentoConsultaService documentoConsultaService) {
        this.documentoConsultaService = documentoConsultaService;
    }

    @Acceso("CLIENTES:CONSULTAR_DOCUMENTOS")
    @GetMapping("/dni/{numero}")
    public ResponseEntity<DocumentoConsultaResponseDTO> consultarDni(
        @PathVariable @Pattern(
            regexp = "[0-9]{8}",
            message = "El DNI debe contener exactamente 8 dígitos"
        ) String numero
    ) {
        return ResponseEntity.ok(documentoConsultaService.consultarDni(numero));
    }

    @Acceso("CLIENTES:CONSULTAR_DOCUMENTOS")
    @GetMapping("/ruc/{numero}")
    public ResponseEntity<DocumentoConsultaResponseDTO> consultarRuc(
        @PathVariable @Pattern(
            regexp = "[0-9]{11}",
            message = "El RUC debe contener exactamente 11 dígitos"
        ) String numero
    ) {
        return ResponseEntity.ok(documentoConsultaService.consultarRuc(numero));
    }
}
