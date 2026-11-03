package pe.com.rebolsysBackend.service.service;

import java.util.Map;
import pe.com.rebolsysBackend.dto.*;

public interface EmisionElectronicaService {
  EmisionElectronicaDTO preparar(Long id,EmisionElectronicaRequestDTO r);
  EmisionElectronicaDTO obtener(Long id);
  EmisionElectronicaDTO enviar(Long id,OperacionUsuarioDTO r);
  EmisionElectronicaDTO consultar(Long id,OperacionUsuarioDTO r);
  byte[] archivo(Long id,String tipo);
  Map<String,Object> configuracion();
}
