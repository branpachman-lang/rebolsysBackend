package pe.com.rebolsysBackend.service.service;

import pe.com.rebolsysBackend.dto.*;

public interface CuentaVentaService {
  CuentaDocumentoDTO cuenta(Long idVenta);
  CuentaDocumentoDTO pagar(Long idVenta, PagoCuentaDTO request);
  CuentaDocumentoDTO emitir(Long idVenta, ComprobanteCuentaDTO request);
}
