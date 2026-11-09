package pe.com.rebolsysBackend.controller;

import pe.com.rebolsysBackend.config.Acceso;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.service.service.CuentaVentaService;

@RestController
@RequestMapping("/api/v1/ventas/{idVenta}")
@RequiredArgsConstructor
@Tag(name = "Ventas de productos")
public class CuentaVentaController {
  private final CuentaVentaService servicio;

  @Acceso("VENTAS:VER")
  @GetMapping("/cuenta")
  @Operation(summary = "Consultar total, pagos y saldo de una venta separada")
  public CuentaDocumentoDTO cuenta(@PathVariable Long idVenta) {
    return servicio.cuenta(idVenta);
  }

  @Acceso("VENTAS:COBRAR")
  @PostMapping("/pagos")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Registrar pago parcial o completo de una venta separada")
  public CuentaDocumentoDTO pagar(@PathVariable Long idVenta, @Valid @RequestBody PagoCuentaDTO request) {
    return servicio.pagar(idVenta, request);
  }

  @Acceso("VENTAS:EMITIR_COMPROBANTE")
  @PostMapping("/comprobantes")
  @Operation(summary = "Emitir boleta o factura interna de una venta separada")
  public CuentaDocumentoDTO emitir(@PathVariable Long idVenta, @Valid @RequestBody ComprobanteCuentaDTO request) {
    return servicio.emitir(idVenta, request);
  }
}
