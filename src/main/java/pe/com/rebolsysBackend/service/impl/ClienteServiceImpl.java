package pe.com.rebolsysBackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.ClienteRegistroRequestDTO;
import pe.com.rebolsysBackend.dto.ClienteResumenResponseDTO;
import pe.com.rebolsysBackend.entity.Cliente;
import pe.com.rebolsysBackend.entity.Perfil;
import pe.com.rebolsysBackend.entity.PersonaJuridica;
import pe.com.rebolsysBackend.entity.PersonaNatural;
import pe.com.rebolsysBackend.entity.TipoDocumento;
import pe.com.rebolsysBackend.exception.RecursoNoEncontradoException;
import pe.com.rebolsysBackend.exception.ReglaNegocioException;
import pe.com.rebolsysBackend.repository.ClienteRepository;
import pe.com.rebolsysBackend.repository.PerfilRepository;
import pe.com.rebolsysBackend.repository.PersonaJuridicaRepository;
import pe.com.rebolsysBackend.repository.PersonaNaturalRepository;
import pe.com.rebolsysBackend.repository.TipoDocumentoRepository;
import pe.com.rebolsysBackend.service.service.ClienteService;

@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

  private final TipoDocumentoRepository tiposDocumento;
  private final PersonaJuridicaRepository juridicas;
  private final PersonaNaturalRepository naturales;
  private final PerfilRepository perfiles;
  private final ClienteRepository clientes;
  private final ReglasPropuesta reglas;

  @Override
  @Transactional(readOnly = true)
  public ClienteResumenResponseDTO buscar(Long tipo, String numero) {
    var perfil =
        perfiles
            .findByTipoDocumentoIdTipoDocumentoAndNumeroDocumento(tipo, numero.trim())
            .orElseThrow(
                () ->
                    new RecursoNoEncontradoException(
                        "No hay un cliente registrado con ese documento. Completa sus datos."));
    return new ClienteResumenResponseDTO(
        clientes.findByPerfilIdPerfil(perfil.getIdPerfil()).map(Cliente::getIdCliente).orElse(null),
        datos(perfil));
  }

  @Override
  @Transactional(readOnly = true)
  public ClienteRegistroRequestDTO datos(Perfil perfil) {
    var natural = naturales.findById(perfil.getIdPerfil()).orElse(null);
    var empresa = juridicas.findById(perfil.getIdPerfil()).orElse(null);
    return new ClienteRegistroRequestDTO(
        perfil.getTipoDocumento().getIdTipoDocumento(),
        perfil.getNumeroDocumento(),
        empresa != null,
        natural == null ? null : natural.getNombres(),
        natural == null ? null : natural.getApellidos(),
        natural == null ? null : natural.getFechaNacimiento(),
        empresa == null ? null : empresa.getRazonSocial(),
        empresa == null ? null : empresa.getDireccionFiscal(),
        perfil.getCorreo(),
        perfil.getTelefono());
  }

  @Override
  @Transactional
  public Perfil guardarPersona(ClienteRegistroRequestDTO datos) {
    TipoDocumento tipo = tiposDocumento.findById(datos.getIdTipoDocumento()).orElse(null);
    if (tipo == null || "INTERNO".equalsIgnoreCase(tipo.getNombreDocumento()))
      throw new ReglaNegocioException("Selecciona un tipo de documento válido");
    var found =
        perfiles.findByTipoDocumentoIdTipoDocumentoAndNumeroDocumento(
            datos.getIdTipoDocumento(), datos.getNumeroDocumento().trim());
    if (found.isPresent()) {
      var perfil = found.get();
      boolean juridica = juridicas.findById(perfil.getIdPerfil()).orElse(null) != null;
      if (juridica != Boolean.TRUE.equals(datos.getEmpresa()))
        throw new ReglaNegocioException("El documento ya corresponde a otro tipo de persona");
      if (!juridica) {
        var natural = naturales.findById(perfil.getIdPerfil()).orElseThrow();
        if (natural.getFechaNacimiento() == null && datos.getFechaNacimiento() != null) {
          natural.setFechaNacimiento(datos.getFechaNacimiento());
          naturales.saveAndFlush(natural);
        } else if (datos.getFechaNacimiento() != null
            && !datos.getFechaNacimiento().equals(natural.getFechaNacimiento())) {
          throw new ReglaNegocioException(
              "La fecha de nacimiento no coincide con la persona registrada; revisa sus datos");
        }
      }
      return perfil;
    }
    var perfil = new Perfil();
    perfil.setTipoDocumento(tipo);
    perfil.setNumeroDocumento(datos.getNumeroDocumento().trim());
    perfil.setCorreo(limpiar(datos.getCorreo()));
    perfil.setTelefono(limpiar(datos.getTelefono()));
    perfiles.save(perfil);
    if (Boolean.TRUE.equals(datos.getEmpresa())) {
      var empresa = new PersonaJuridica();
      empresa.setPerfil(perfil);
      empresa.setRazonSocial(datos.getRazonSocial().trim());
      empresa.setDireccionFiscal(limpiar(datos.getDireccionFiscal()));
      juridicas.save(empresa);
    } else {
      var natural = new PersonaNatural();
      natural.setPerfil(perfil);
      natural.setNombres(datos.getNombres().trim());
      natural.setApellidos(datos.getApellidos().trim());
      natural.setFechaNacimiento(datos.getFechaNacimiento());
      naturales.save(natural);
    }
    return perfil;
  }

  @Override
  @Transactional
  public Cliente guardarCliente(ClienteRegistroRequestDTO datos) {
    Perfil perfil = guardarPersona(datos);
    return clientes
        .findByPerfilIdPerfil(perfil.getIdPerfil())
        .orElseGet(
            () -> {
              var cliente = new Cliente();
              cliente.setPerfil(perfil);
              return clientes.save(cliente);
            });
  }

  @Override
  @Transactional(readOnly = true)
  public String nombre(Perfil perfil) {
    ClienteRegistroRequestDTO datos = datos(perfil);
    return Boolean.TRUE.equals(datos.getEmpresa())
        ? datos.getRazonSocial()
        : datos.getNombres() + " " + datos.getApellidos();
  }

  @Override
  @Transactional
  public Perfil actualizarPersona(Long idPerfil, ClienteRegistroRequestDTO datos) {
    Perfil perfil =
        perfiles
            .findById(idPerfil)
            .orElseThrow(() -> new RecursoNoEncontradoException("La persona no existe"));
    var existente =
        perfiles.findByTipoDocumentoIdTipoDocumentoAndNumeroDocumento(
            datos.getIdTipoDocumento(), datos.getNumeroDocumento().trim());
    if (existente.isPresent() && !existente.get().getIdPerfil().equals(idPerfil))
      throw new ReglaNegocioException("El documento ya pertenece a otra persona registrada");
    var natural = naturales.findById(idPerfil).orElse(null);
    var juridica = juridicas.findById(idPerfil).orElse(null);
    if (Boolean.TRUE.equals(datos.getEmpresa()) != (juridica != null))
      throw new ReglaNegocioException(
          "No se puede convertir una persona natural en empresa ni una empresa en persona natural"
              + " desde la reserva");
    perfil.setTipoDocumento(
        tiposDocumento
            .findById(datos.getIdTipoDocumento())
            .orElseThrow(() -> new RecursoNoEncontradoException("El tipo de documento no existe")));
    perfil.setNumeroDocumento(datos.getNumeroDocumento().trim());
    perfil.setCorreo(limpiar(datos.getCorreo()));
    perfil.setTelefono(limpiar(datos.getTelefono()));
    if (natural != null) {
      natural.setNombres(datos.getNombres().trim());
      natural.setApellidos(datos.getApellidos().trim());
      natural.setFechaNacimiento(datos.getFechaNacimiento());
      naturales.save(natural);
    } else {
      juridica.setRazonSocial(datos.getRazonSocial().trim());
      juridica.setDireccionFiscal(limpiar(datos.getDireccionFiscal()));
      juridicas.save(juridica);
    }
    return perfiles.save(perfil);
  }

  private String limpiar(String texto) {
    return texto == null || texto.isBlank() ? null : texto.trim();
  }

  @Override
  @Transactional(readOnly = true)
  public pe.com.rebolsysBackend.dto.ClienteDetalleDTO obtener(Long id) {
    var c =
        clientes
            .findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("El cliente no existe"));
    return new pe.com.rebolsysBackend.dto.ClienteDetalleDTO(
        c.getIdCliente(), datos(c.getPerfil()), c.getRestriccionesMedicas());
  }

  @Override
  @Transactional
  public pe.com.rebolsysBackend.dto.ClienteDetalleDTO guardar(
      Long id, pe.com.rebolsysBackend.dto.ClienteGuardarDTO r) {
    reglas.validar(r);
    Cliente c;
    if (id == null) c = guardarCliente(r.persona());
    else {
      c =
          clientes
              .bloquear(id)
              .orElseThrow(() -> new RecursoNoEncontradoException("El cliente no existe"));
      actualizarPersona(c.getPerfil().getIdPerfil(), r.persona());
    }
    c.setRestriccionesMedicas(limpiar(r.restriccionesMedicas()));
    clientes.saveAndFlush(c);
    return obtener(c.getIdCliente());
  }
}
