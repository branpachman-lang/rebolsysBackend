package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.*;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.enums.*;
import pe.com.rebolsysBackend.exception.*;
import pe.com.rebolsysBackend.repository.*;
import pe.com.rebolsysBackend.service.service.*;

@Service
@RequiredArgsConstructor
public class CatalogoServiceImpl implements CatalogoService {
  private final TipoDocumentoRepository documentos;
  private final FormaPagoRepository pagos;
  private final TipoHabitacionRepository habitaciones;
  private final TipoItemRepository items;
  private final TipoMovimientoCajaRepository gastos;
  private final UsuarioRepository usuarios;
  private final TurnoCajaRepository turnos;

  @Override
  @Transactional(readOnly = true)
  public CatalogosResponseDTO catalogos() {
    return new CatalogosResponseDTO(
        documentos.findByNombreDocumentoNotOrderByIdTipoDocumento("INTERNO").stream()
            .map(
                t ->
                    new CatalogoItemResponseDTO(
                        t.getIdTipoDocumento(), t.getNombreDocumento(), t.getCodigoSunat()))
            .toList(),
        pagos.findAll().stream()
            .map(t -> new CatalogoItemResponseDTO(t.getIdFormaPago(), t.getNombrePago()))
            .toList(),
        habitaciones.findAll().stream()
            .map(t -> new CatalogoItemResponseDTO(t.getIdTipoHabitacion(), t.getNombreTipo()))
            .toList(),
        items.findAll().stream()
            .map(t -> new CatalogoItemResponseDTO(t.getIdTipoItem(), t.getNombreItem()))
            .toList(),
        gastos.findAll().stream()
            .filter(t -> t.getSigno() == -1)
            .map(t -> new CatalogoItemResponseDTO(t.getIdTipoMovimiento(), t.getNombreMovimiento()))
            .toList(),
        usuarios.findAll().stream()
            .filter(t -> t.getEstadoUsuario() == EstadoUsuario.ACTIVO)
            .map(t -> new CatalogoItemResponseDTO(t.getIdUsuario(), t.getUsername()))
            .toList(),
        turnos.findAll().stream()
            .filter(t -> t.getEstadoSesion() == 1 && t.getFechaCierre() == null)
            .map(
                t ->
                    new TurnoDisponibleDTO(
                        t.getIdTurno(),
                        t.getUsuario().getIdUsuario(),
                        t.getUsuario().getUsername(),
                        t.getFechaApertura()))
            .toList());
  }
}
