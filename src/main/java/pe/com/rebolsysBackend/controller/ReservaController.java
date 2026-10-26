package pe.com.rebolsysBackend.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.rebolsysBackend.dto.CancelacionReservaRequestDTO;
import pe.com.rebolsysBackend.dto.ComprobanteInternoResponseDTO;
import pe.com.rebolsysBackend.dto.PaginaResponseDTO;
import pe.com.rebolsysBackend.dto.PagoEfectivoRequestDTO;
import pe.com.rebolsysBackend.dto.ReservaRegistroRequestDTO;
import pe.com.rebolsysBackend.dto.ReservaResumenResponseDTO;
import pe.com.rebolsysBackend.service.service.ReservaService;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponseDTO<ReservaResumenResponseDTO>> readAll(
        @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página debe ser mayor o igual a 0") int pagina,
        @RequestParam(defaultValue = "10") @Min(value = 1, message = "El tamaño debe ser mayor que 0") @Max(
            value = 100,
            message = "El tamaño no debe superar 100"
        ) int tamanio,
        @RequestParam(defaultValue = "") String buscar,
        @RequestParam(defaultValue = "") @jakarta.validation.constraints.Pattern(
            regexp = "^$|CONFIRMADA|EN_CURSO|FINALIZADA|CANCELADA",
            message = "Selecciona un estado de reserva válido"
        ) String estado
    ) {
        return ResponseEntity.ok(reservaService.listar(pagina, tamanio, buscar, estado));
    }

    @GetMapping("/{ref}")
    public ResponseEntity<ReservaResumenResponseDTO> read(@PathVariable String ref) {
        return ResponseEntity.ok(reservaService.read(ref));
    }

    @PostMapping
    public ResponseEntity<ReservaResumenResponseDTO> create(@Valid @RequestBody ReservaRegistroRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.create(request));
    }

    @PutMapping("/{ref}")
    public ResponseEntity<ReservaResumenResponseDTO> update(
        @PathVariable String ref,
        @Valid @RequestBody ReservaRegistroRequestDTO request
    ) {
        return ResponseEntity.ok(reservaService.update(ref, request));
    }

    @DeleteMapping("/{ref}")
    public ResponseEntity<Void> delete(@PathVariable String ref) {
        reservaService.delete(ref);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ref}/pago")
    public ResponseEntity<ReservaResumenResponseDTO> pagar(
        @PathVariable String ref,
        @Valid @RequestBody PagoEfectivoRequestDTO request
    ) {
        return ResponseEntity.ok(reservaService.pagar(ref, request));
    }

    @PostMapping("/{ref}/cancelacion")
    public ResponseEntity<ReservaResumenResponseDTO> cancelar(
        @PathVariable String ref,
        @Valid @RequestBody CancelacionReservaRequestDTO request
    ) {
        return ResponseEntity.ok(reservaService.cancelar(ref, request));
    }

    @GetMapping("/{ref}/comprobante")
    public ResponseEntity<ComprobanteInternoResponseDTO> comprobante(
        @PathVariable String ref,
        @RequestParam(required = false) Long idComprobante
    ) {
        return ResponseEntity.ok(reservaService.comprobante(ref, idComprobante));
    }
}
