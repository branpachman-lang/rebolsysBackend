package pe.com.rebolsysBackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.service.service.EmisionElectronicaService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name="Facturación electrónica beta")
public class EmisionElectronicaController {
  private final EmisionElectronicaService servicio;

  @GetMapping("/facturacion/configuracion")
  @Operation(summary="Consultar la habilitación y los datos pendientes de facturación, sin credenciales")
  public Map<String,Object> configuracion() { return servicio.configuracion(); }

  @PostMapping("/comprobantes/{id}/emision-electronica")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary="Preparar una boleta o factura beta desde un comprobante interno emitido",
      description="Los precios incluyen impuesto. La tasa se indica como porcentaje; se pueden especificar tratamientos distintos por línea. No envía a APIsPerú todavía.")
  public EmisionElectronicaDTO preparar(@PathVariable Long id,@Valid @RequestBody EmisionElectronicaRequestDTO r) {
    return servicio.preparar(id,r);
  }
  @GetMapping("/comprobantes/{id}/emision-electronica")
  @Operation(summary="Consultar el estado persistido de la emisión beta")
  public EmisionElectronicaDTO obtener(@PathVariable Long id) { return servicio.obtener(id); }

  @PostMapping("/comprobantes/{id}/emision-electronica/envio")
  @Operation(summary="Enviar a APIsPerú una copia fiscal preparada, con empresa verificada en beta")
  public EmisionElectronicaDTO enviar(@PathVariable Long id,@Valid @RequestBody OperacionUsuarioDTO r) {
    return servicio.enviar(id,r);
  }
  @PostMapping("/comprobantes/{id}/emision-electronica/consulta")
  @Operation(summary="Consultar el CDR de una emisión sin volver a emitir")
  public EmisionElectronicaDTO consultar(@PathVariable Long id,@Valid @RequestBody OperacionUsuarioDTO r) {
    return servicio.consultar(id,r);
  }
  @GetMapping("/comprobantes/{id}/emision-electronica/xml")
  @Operation(summary="Descargar el XML firmado recibido del proveedor beta")
  public ResponseEntity<byte[]> xml(@PathVariable Long id) { return archivo(id,"xml","application/xml"); }
  @GetMapping("/comprobantes/{id}/emision-electronica/cdr")
  @Operation(summary="Descargar el ZIP del CDR recibido del proveedor beta")
  public ResponseEntity<byte[]> cdr(@PathVariable Long id) { return archivo(id,"cdr","application/zip"); }
  @GetMapping("/comprobantes/{id}/emision-electronica/pdf")
  @Operation(summary="Obtener una representación PDF de prueba desde APIsPerú")
  public ResponseEntity<byte[]> pdf(@PathVariable Long id) { return archivo(id,"pdf","application/pdf"); }

  private ResponseEntity<byte[]> archivo(Long id,String tipo,String media) {
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(media))
        .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"BETA-comprobante-"+id+"."+(tipo.equals("cdr")?"zip":tipo)+"\"")
        .header("X-Comprobante-Ambiente","BETA").body(servicio.archivo(id,tipo));
  }
}
