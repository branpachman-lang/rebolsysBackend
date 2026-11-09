package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.*;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.*;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.exception.*;
import pe.com.rebolsysBackend.repository.*;
import pe.com.rebolsysBackend.service.service.*;

@Service
@RequiredArgsConstructor
public class GastoServiceImpl implements GastoService {
  private final jakarta.persistence.EntityManager entityManager;
  private final FlujoCajaRepository flujos;
  private final TurnoCajaRepository turnos;
  private final TipoMovimientoCajaRepository tipos;
  private final ReglasPropuesta reglas;
  private final UsuarioRepository usuarios;

  @Override
  @Transactional
  public GastoResponseDTO registrar(GastoRequestDTO r) {
    reglas.validar(r);
    reglas.usuario(r.idUsuario());
    var turno =
        turnos
            .bloquear(r.idTurno())
            .orElseThrow(() -> new RecursoNoEncontradoException("El turno no existe"));
    entityManager.refresh(turno);
    exigir(
        turno.getUsuario().getIdUsuario().equals(r.idUsuario()),
        "El turno pertenece a otro usuario");
    if (r.claveRegistro() != null) {
      var previo = flujos.findByClaveRegistro(r.claveRegistro());
      if (previo.isPresent()) {
        exigir(mismaSolicitud(previo.get(), r), "La clave de registro ya corresponde a otro gasto");
        return respuesta(previo.get());
      }
    }
    exigir(
        turno.getEstadoSesion() == 1 && turno.getFechaCierre() == null,
        "El turno de caja debe estar abierto");
    var tipo =
        tipos
            .findById(r.idTipoMovimiento())
            .orElseThrow(() -> new RecursoNoEncontradoException("El tipo de gasto no existe"));
    exigir(tipo.getSigno() == -1, "El tipo de movimiento debe corresponder a una salida de caja");
    exigir(!EvidenciaCajaService.esRutaCaja(limpiar(r.evidenciaRuta())), "Adjunta el archivo desde el módulo de Caja");
    exigir(
        (limpiar(r.evidenciaRuta()) == null) == (r.evidenciaSha256() == null),
        "Indica la ruta y la huella de la evidencia juntas");
    var f = new FlujoCaja();
    f.setTurnoCaja(turno);
    f.setTipoMovimientoCaja(tipo);
    f.setConcepto(r.concepto().trim());
    f.setMonto(r.monto());
    f.setDocumentoRespaldo(limpiar(r.documentoRespaldo()));
    f.setEvidenciaRuta(limpiar(r.evidenciaRuta()));
    f.setEvidenciaSha256(huella(r.evidenciaSha256()));
    f.setClaveRegistro(r.claveRegistro());
    return respuesta(flujos.saveAndFlush(f));
  }

  private boolean esGasto(FlujoCaja f) {
    return f.getPago() == null && f.getTipoMovimientoCaja().getSigno() == -1;
  }

  @Override
  @Transactional(readOnly = true)
  public GastoResponseDTO obtener(Long id) {
    var f =
        flujos
            .findById(id)
            .filter(this::esGasto)
            .orElseThrow(() -> new RecursoNoEncontradoException("El gasto no existe"));
    return respuesta(f);
  }

  @Override
  @Transactional(readOnly = true)
  public List<GastoResponseDTO> listar(Long idTurno) {
    if (!turnos.existsById(idTurno)) throw new RecursoNoEncontradoException("El turno no existe");
    return flujos.findByTurnoCajaIdTurnoOrderByIdFlujoDesc(idTurno).stream()
        .filter(this::esGasto)
        .map(this::respuesta)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public GastoHistorialDTO historial(GastoFiltroDTO f) {
    reglas.validar(f);
    if (f.idTurno() != null && !turnos.existsById(f.idTurno()))
      throw new RecursoNoEncontradoException("El turno no existe");
    if (f.idUsuario() != null && !usuarios.existsById(f.idUsuario()))
      throw new RecursoNoEncontradoException("El usuario no existe");
    if (f.idTipoMovimiento() != null) {
      var tipo = tipos.findById(f.idTipoMovimiento())
          .orElseThrow(() -> new RecursoNoEncontradoException("El tipo de gasto no existe"));
      exigir(tipo.getSigno() == -1, "El tipo seleccionado no corresponde a un gasto");
    }
    var filtro = GastoEspecificaciones.filtrar(f);
    var pagina = flujos.findAll(filtro, PageRequest.of(f.pagina(), f.tamanio(),
        Sort.by(Sort.Direction.DESC, "fechaHora", "idFlujo")));
    var gastos = new PaginaResponseDTO<>(pagina.getContent().stream().map(this::respuesta).toList(),
        pagina.getNumber(), pagina.getSize(), pagina.getTotalElements(), pagina.getTotalPages(), pagina.isLast());
    return new GastoHistorialDTO(gastos, flujos.resumir(filtro));
  }

  @Override
  @Transactional(readOnly = true)
  public List<CatalogoItemResponseDTO> tipos() {
    return tipos.findAll(Sort.by("nombreMovimiento")).stream().filter(t -> t.getSigno() == -1)
        .map(t -> new CatalogoItemResponseDTO(t.getIdTipoMovimiento(), t.getNombreMovimiento())).toList();
  }

  private boolean mismaSolicitud(FlujoCaja f, GastoRequestDTO r) {
    return esGasto(f) && f.getTurnoCaja().getIdTurno().equals(r.idTurno())
        && f.getTurnoCaja().getUsuario().getIdUsuario().equals(r.idUsuario())
        && f.getTipoMovimientoCaja().getIdTipoMovimiento().equals(r.idTipoMovimiento())
        && f.getConcepto().equals(r.concepto().trim()) && f.getMonto().compareTo(r.monto()) == 0
        && Objects.equals(f.getDocumentoRespaldo(), limpiar(r.documentoRespaldo()))
        && Objects.equals(f.getEvidenciaRuta(), limpiar(r.evidenciaRuta()))
        && Objects.equals(f.getEvidenciaSha256(), huella(r.evidenciaSha256()));
  }

  private String huella(String valor) {
    return valor == null ? null : valor.toLowerCase(Locale.ROOT);
  }

  private GastoResponseDTO respuesta(FlujoCaja f) {
    return new GastoResponseDTO(
        f.getIdFlujo(),
        f.getTurnoCaja().getIdTurno(),
        f.getTurnoCaja().getUsuario().getIdUsuario(),
        f.getTipoMovimientoCaja().getIdTipoMovimiento(),
        f.getConcepto(),
        f.getMonto(),
        f.getFechaHora(),
        f.getDocumentoRespaldo(),
        f.getEvidenciaRuta(),
        f.getEvidenciaSha256(),
        f.getTurnoCaja().getUsuario().getUsername(),
        f.getTipoMovimientoCaja().getNombreMovimiento(),
        f.getTurnoCaja().getEstadoSesion() == 1 && f.getTurnoCaja().getFechaCierre() == null,
        f.getClaveRegistro());
  }
}
