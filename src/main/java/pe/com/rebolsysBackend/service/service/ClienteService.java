package pe.com.rebolsysBackend.service.service;

import pe.com.rebolsysBackend.dto.ClienteRegistroRequestDTO;
import pe.com.rebolsysBackend.dto.ClienteResumenResponseDTO;
import pe.com.rebolsysBackend.entity.Cliente;
import pe.com.rebolsysBackend.entity.Perfil;

public interface ClienteService {
  ClienteResumenResponseDTO buscar(Long tipoDocumento, String numeroDocumento);

  ClienteRegistroRequestDTO datos(Perfil perfil);

  Perfil actualizarPersona(Long idPerfil, ClienteRegistroRequestDTO request);

  Perfil guardarPersona(ClienteRegistroRequestDTO request);

  Cliente guardarCliente(ClienteRegistroRequestDTO request);

  String nombre(Perfil perfil);

  pe.com.rebolsysBackend.dto.ClienteDetalleDTO obtener(Long id);

  pe.com.rebolsysBackend.dto.ClienteDetalleDTO guardar(
      Long id, pe.com.rebolsysBackend.dto.ClienteGuardarDTO r);
}
