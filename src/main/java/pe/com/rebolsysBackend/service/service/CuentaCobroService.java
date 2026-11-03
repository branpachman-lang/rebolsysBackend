package pe.com.rebolsysBackend.service.service;

import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.entity.Comprobante;

public interface CuentaCobroService {
  CuentaDocumentoDTO documento(Comprobante cuenta);
  Comprobante pagar(Comprobante cuenta, PagoCuentaDTO request);
  Comprobante emitir(Comprobante cuenta, ComprobanteCuentaDTO request);
}
