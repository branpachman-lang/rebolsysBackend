package pe.com.rebolsysBackend.controller;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.service.service.CuentaReservaService;

@RestController
@RequestMapping("/api/v1/reservas/{idReserva}")
@RequiredArgsConstructor
@Tag(name = "Reservas")
public class CuentaReservaController {
  private final CuentaReservaService servicio;

  @GetMapping("/cuenta")
  @Operation(summary = "Consultar alojamiento, consumos, pagos y saldo de la reserva")
  public CuentaReservaDTO cuenta(@PathVariable Long idReserva) {
    return servicio.cuenta(idReserva);
  }

  @PostMapping("/pagos")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Registrar un anticipo o pago de alojamiento o consumo separado")
  public CuentaReservaDTO pagar(
      @PathVariable Long idReserva, @Valid @RequestBody ReservaPagoRegistroDTO request) {
    return servicio.pagar(idReserva, request);
  }

  @PostMapping("/comprobantes")
  @Operation(summary = "Emitir registro interno de boleta o factura de una cuenta",
      description = "Conserva pagos y número de reserva. Esta etapa no integra SUNAT.")
  public CuentaDocumentoDTO emitir(
      @PathVariable Long idReserva, @Valid @RequestBody ReservaComprobanteRegistroDTO request) {
    return servicio.emitir(idReserva, request);
  }

  @PostMapping("/consumos")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Registrar y confirmar consumo de una habitación de la reserva")
  public ConsumoReservaRespuestaDTO consumir(
      @PathVariable Long idReserva, @Valid @RequestBody VentaRequestDTO request) {
    return servicio.consumir(idReserva, request);
  }

  @GetMapping("/consumos")
  @Operation(summary = "Consultar consumos consolidados y separados de la reserva")
  public List<ConsumoReservaRespuestaDTO> consumos(@PathVariable Long idReserva) {
    return servicio.consumos(idReserva);
  }
}
