package pe.com.rebolsysBackend.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.CancelacionReservaRequestDTO;
import pe.com.rebolsysBackend.dto.ClienteResumenResponseDTO;
import pe.com.rebolsysBackend.dto.ComprobanteInternoResponseDTO;
import pe.com.rebolsysBackend.dto.PaginaResponseDTO;
import pe.com.rebolsysBackend.dto.PagoEfectivoRequestDTO;
import pe.com.rebolsysBackend.dto.ReservaDetalleRequestDTO;
import pe.com.rebolsysBackend.dto.ReservaDetalleResponseDTO;
import pe.com.rebolsysBackend.dto.ReservaHuespedRegistroRequestDTO;
import pe.com.rebolsysBackend.dto.ReservaRegistroRequestDTO;
import pe.com.rebolsysBackend.dto.ReservaResumenResponseDTO;
import pe.com.rebolsysBackend.dto.ResponsableHuespedRequestDTO;
import pe.com.rebolsysBackend.entity.Consumo;
import pe.com.rebolsysBackend.entity.DetalleReserva;
import pe.com.rebolsysBackend.entity.Habitacion;
import pe.com.rebolsysBackend.entity.Huesped;
import pe.com.rebolsysBackend.entity.Perfil;
import pe.com.rebolsysBackend.entity.Reserva;
import pe.com.rebolsysBackend.entity.ReservaHuesped;
import pe.com.rebolsysBackend.entity.Usuario;
import pe.com.rebolsysBackend.entity.id.ReservaHuespedId;
import pe.com.rebolsysBackend.enums.EstadoCargo;
import pe.com.rebolsysBackend.enums.EstadoCobro;
import pe.com.rebolsysBackend.enums.EstadoHabitacion;
import pe.com.rebolsysBackend.enums.EstadoReserva;
import pe.com.rebolsysBackend.enums.FormaRegistro;
import pe.com.rebolsysBackend.enums.TipoConsumo;
import pe.com.rebolsysBackend.exception.RecursoNoEncontradoException;
import pe.com.rebolsysBackend.exception.ReglaNegocioException;
import pe.com.rebolsysBackend.repository.ConsumoRepository;
import pe.com.rebolsysBackend.repository.DetalleReservaRepository;
import pe.com.rebolsysBackend.repository.HabitacionRepository;
import pe.com.rebolsysBackend.repository.HuespedRepository;
import pe.com.rebolsysBackend.repository.PersonaNaturalRepository;
import pe.com.rebolsysBackend.repository.ReservaHuespedRepository;
import pe.com.rebolsysBackend.repository.ReservaRepository;
import pe.com.rebolsysBackend.repository.TipoOcupanteRepository;
import pe.com.rebolsysBackend.repository.UsuarioRepository;
import pe.com.rebolsysBackend.service.service.ClienteService;
import pe.com.rebolsysBackend.service.service.ComprobanteService;
import pe.com.rebolsysBackend.service.service.PagoService;
import pe.com.rebolsysBackend.service.service.ReservaService;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    private final TipoOcupanteRepository tiposOcupante;
    private final PersonaNaturalRepository naturales;
    private final UsuarioRepository usuarios;
    private final EntityManager em;
    private final Validator validator;
    private final DetalleReservaRepository detalleReservaRepository;
    private final ReservaRepository reservaRepository;
    private final HabitacionRepository habitaciones;
    private final ConsumoRepository consumos;
    private final HuespedRepository huespedes;
    private final ReservaHuespedRepository ocupantes;
    private final ClienteService clientes;
    private final ObjectMapper json;
    private final PagoService pagoService;
    private final ComprobanteService comprobanteService;

    @Override
    @Transactional
    public ReservaResumenResponseDTO create(ReservaRegistroRequestDTO request) {
        validarSolicitud(request);
        var existing = reservaRepository.findByClaveRegistro(request.getClaveRegistro());
        if (existing.isPresent()) return reintento(existing.get(), request);
        Map<Long, Habitacion> rooms = bloquearHabitaciones(request.getHabitaciones());
        existing = reservaRepository.findByClaveRegistro(request.getClaveRegistro());
        if (existing.isPresent()) return reintento(existing.get(), request);
        validarEstancias(request.getHabitaciones(), rooms, null, false);
        var reserva = new Reserva();
        reserva.setClaveRegistro(request.getClaveRegistro());
        reserva.setFirmaSolicitud(firma(request));
        reserva.setCliente(clientes.guardarCliente(request.getCliente()));
        reserva.setUsuario(obtenerUsuarioReserva());
        reservaRepository.saveAndFlush(reserva);
        List<DetalleReserva> rows = new ArrayList<>();
        for (var line : request.getHabitaciones())
            rows.add(crearLinea(reserva, rooms.get(line.getIdHabitacion()), line));
        BigDecimal vuelto = BigDecimal.ZERO;
        if (request.getFormaRegistro() == FormaRegistro.EFECTIVO) vuelto = pagoService.cobrar(
            rows,
            "registro:" + request.getClaveRegistro(),
            request.getEfectivoRecibido(),
            request.getMontoPago()
        );
        em.flush();
        return convertirResponse(rows, vuelto);
    }

    private ReservaResumenResponseDTO reintento(Reserva reserva, ReservaRegistroRequestDTO request) {
        if (!reserva.getFirmaSolicitud().equals(firma(request))) throw new ReglaNegocioException(
            "Esta solicitud ya fue registrada con datos distintos. Actualiza la lista antes de continuar."
        );
        return convertirResponse(visibles(reserva.getDetalles()), BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public ReservaResumenResponseDTO update(String ref, ReservaRegistroRequestDTO request) {
        validarSolicitud(request);
        List<DetalleReserva> rows = cargar(ref, true);
        validarEditable(rows);
        boolean conPagos = comprobanteService.pagado(rows).signum() > 0;
        if (
            conPagos &&
            (rows.size() != request.getHabitaciones().size() ||
                request
                    .getHabitaciones()
                    .stream()
                    .anyMatch(h ->
                        rows
                            .stream()
                            .noneMatch(
                                r ->
                                    r.getHabitacion().getIdHabitacion().equals(h.getIdHabitacion()) &&
                                    r.getFechaIngreso().equals(h.getFechaIngreso()) &&
                                    r.getFechaSalida().equals(h.getFechaSalida())
                            )
                    ))
        ) throw new ReglaNegocioException(
            "Con pagos registrados solo se pueden editar el cliente y los huéspedes; conserva habitaciones, fechas e importes"
        );
        Map<Long, Long> perfilesPermitidos = new HashMap<>();
        for (DetalleReserva row : rows) {
            var anterioresOcupantes = ocupantes.findByDetalleReservaIdDetalleReserva(row.getIdDetalleReserva());
            anterioresOcupantes.forEach(o ->
                perfilesPermitidos.put(o.getHuesped().getIdHuesped(), o.getHuesped().getPersonaNatural().getIdPerfil())
            );
            if (
                request
                    .getHabitaciones()
                    .stream()
                    .anyMatch(h -> h.getIdHabitacion().equals(row.getHabitacion().getIdHabitacion()))
            ) {
                ocupantes.deleteByDetalleReservaIdDetalleReserva(row.getIdDetalleReserva());
                anterioresOcupantes.forEach(em::detach);
            }
        }
        clientes.actualizarPersona(
            rows.getFirst().getReserva().getCliente().getPerfil().getIdPerfil(),
            request.getCliente()
        );
        Map<Long, Habitacion> rooms = bloquearHabitaciones(request.getHabitaciones());
        Reserva cabecera = rows.getFirst().getReserva();
        validarEstancias(request.getHabitaciones(), rooms, cabecera.getIdReserva(), conPagos);
        Map<Long, DetalleReserva> anteriores = new HashMap<>();
        rows.forEach(r -> anteriores.put(r.getHabitacion().getIdHabitacion(), r));
        Set<Long> seleccionadas = rooms.keySet();
        for (DetalleReserva row : rows)
            if (!seleccionadas.contains(row.getHabitacion().getIdHabitacion())) validarSinProductos(
                java.util.List.of(row)
            );
        for (DetalleReserva row : rows) if (!seleccionadas.contains(row.getHabitacion().getIdHabitacion())) anular(row);
        List<DetalleReserva> result = new ArrayList<>();
        for (var line : request.getHabitaciones()) {
            var row = anteriores.get(line.getIdHabitacion());
            if (row == null) row = crearLinea(cabecera, rooms.get(line.getIdHabitacion()), line);
            else {
                em.flush();
                row.setFechaIngreso(line.getFechaIngreso());
                row.setFechaSalida(line.getFechaSalida());
                if (!conPagos) row.setCostoEstadia(costo(rooms.get(line.getIdHabitacion()), line));
                row.setEstadoDetalle(EstadoReserva.CONFIRMADA);
                var cargo = cargo(row);
                if (!conPagos) completarCargo(cargo, row);
                em.flush();
                guardarOcupantes(row, line.getHuespedes(), perfilesPermitidos);
            }
            result.add(row);
        }
        em.flush();
        sincronizarEstado(cabecera);
        return convertirResponse(result, BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public void delete(String ref) {
        var rows = cargar(ref, true);
        validarEditable(rows);
        validarSinProductos(rows);
        if (comprobanteService.pagado(rows).signum() > 0) throw new ReglaNegocioException(
            "Confirma la cancelación con devolución desde el formulario de cancelación"
        );
        rows.forEach(this::anular);
        sincronizarEstado(rows.getFirst().getReserva());
        em.flush();
    }

    private void validarEditable(List<DetalleReserva> rows) {
        if (
            rows.stream().anyMatch(r -> r.getEstadoDetalle() != EstadoReserva.CONFIRMADA)
        ) throw new ReglaNegocioException("Solo se pueden modificar o cancelar reservas confirmadas.");
    }

    private void validarSinProductos(List<DetalleReserva> rows) {
        if (
            rows
                .stream()
                .flatMap(r -> consumos.findByDetalleReservaIdDetalleReserva(r.getIdDetalleReserva()).stream())
                .anyMatch(c -> c.getTipoConsumo() == TipoConsumo.PRODUCTO && c.getEstadoCargo() != EstadoCargo.ANULADO)
        ) throw new ReglaNegocioException(
            "La reserva tiene productos de tienda entregados. Revisa su liquidación antes de retirar habitaciones o realizar una cancelación con devolución"
        );
    }

    private void anular(DetalleReserva row) {
        row.setEstadoDetalle(EstadoReserva.CANCELADA);
        consumos
            .findByDetalleReservaIdDetalleReserva(row.getIdDetalleReserva())
            .forEach(c -> {
                c.setEstadoCargo(EstadoCargo.ANULADO);
                c.setEstadoCobro(EstadoCobro.ANULADO);
            });
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaResumenResponseDTO read(String ref) {
        return convertirResponse(cargar(ref, false), BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResumenResponseDTO> readAll() {
        return reservaRepository
            .findAll(Sort.by(Sort.Direction.DESC, "idReserva"))
            .stream()
            .map(r -> convertirResponse(visibles(r.getDetalles()), BigDecimal.ZERO))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponseDTO<ReservaResumenResponseDTO> listar(int pagina, int tamanio, String buscar, String estado) {
        EstadoReserva filtro = estado == null || estado.isBlank() ? null : EstadoReserva.valueOf(estado);
        String texto = buscar == null ? "" : buscar.trim().toLowerCase(Locale.ROOT);
        if (texto.matches("[0-9]+")) texto = texto.replaceFirst("^0+(?!$)", "");
        var resultado = reservaRepository.buscar(
            texto.isEmpty() ? null : texto,
            filtro,
            PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "idReserva"))
        );
        return new PaginaResponseDTO<>(
            resultado
                .getContent()
                .stream()
                .map(r -> convertirResponse(visibles(r.getDetalles()), BigDecimal.ZERO))
                .toList(),
            pagina,
            tamanio,
            resultado.getTotalElements(),
            resultado.getTotalPages(),
            resultado.isLast()
        );
    }

    @Override
    @Transactional
    public ReservaResumenResponseDTO pagar(String ref, PagoEfectivoRequestDTO request) {
        var rows = cargar(ref, true);
        if (
            rows.stream().anyMatch(r -> r.getEstadoDetalle() == EstadoReserva.CANCELADA)
        ) throw new ReglaNegocioException("No se puede cobrar una reserva cancelada");
        var errores = validator.validate(request);
        if (!errores.isEmpty()) throw new ConstraintViolationException(errores);
        BigDecimal vuelto = pagoService.cobrar(
            rows,
            "pago:" + request.getClavePago(),
            request.getEfectivoRecibido(),
            request.getMontoPago()
        );
        em.flush();
        return convertirResponse(rows, vuelto);
    }

    @Override
    @Transactional
    public ReservaResumenResponseDTO cancelar(String ref, CancelacionReservaRequestDTO request) {
        var errores = validator.validate(request);
        if (!errores.isEmpty()) throw new ConstraintViolationException(errores);
        var rows = cargar(ref, true);
        if (rows.stream().allMatch(r -> r.getEstadoDetalle() == EstadoReserva.CANCELADA)) return convertirResponse(
            rows,
            BigDecimal.ZERO
        );
        validarEditable(rows);
        validarSinProductos(rows);
        BigDecimal cobrado = comprobanteService.pagado(rows);
        if (cobrado.signum() > 0) {
            if (!request.isConfirmarDevolucion()) throw new ReglaNegocioException(
                "Confirma la devolución total del importe cobrado antes de cancelar"
            );
            pagoService.devolver(rows, request.getMotivo());
        }
        rows.forEach(this::anular);
        sincronizarEstado(rows.getFirst().getReserva());
        em.flush();
        return convertirResponse(rows, BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public ComprobanteInternoResponseDTO comprobante(String referencia) {
        return comprobanteService.obtener(cargar(referencia, false));
    }

    @Override
    @Transactional(readOnly = true)
    public ComprobanteInternoResponseDTO comprobante(String ref, Long idComprobante) {
        var rows = cargar(ref, false);
        if (idComprobante == null) return comprobanteService.obtener(rows);
        if (
            comprobanteService
                .recibos(rows)
                .stream()
                .noneMatch(c -> c.getIdComprobante().equals(idComprobante))
        ) throw new RecursoNoEncontradoException("El comprobante no pertenece a esta reserva");
        return comprobanteService.obtenerPorId(idComprobante);
    }

    private List<DetalleReserva> cargar(String ref, boolean lock) {
        if (ref == null || !ref.matches("[GR][0-9]+")) throw new RecursoNoEncontradoException("Reserva no encontrada");
        long id;
        try {
            id = Long.parseLong(ref.substring(1));
        } catch (NumberFormatException ex) {
            throw new RecursoNoEncontradoException("Reserva no encontrada");
        }
        // G se admite como alias de las referencias anteriores a la migración.
        Reserva reserva = (lock ? reservaRepository.bloquear(id) : reservaRepository.findById(id)).orElseThrow(() ->
            new RecursoNoEncontradoException("Reserva no encontrada")
        );
        var detalles = visibles(reserva.getDetalles());
        if (detalles.isEmpty()) throw new ReglaNegocioException("La reserva no tiene habitaciones registradas");
        return detalles;
    }

    private void sincronizarEstado(Reserva reserva) {
        var detalles = visibles(reserva.getDetalles());
        EstadoReserva estado = detalles.stream().allMatch(d -> d.getEstadoDetalle() == EstadoReserva.CANCELADA)
            ? EstadoReserva.CANCELADA
            : detalles.stream().anyMatch(d -> d.getEstadoDetalle() == EstadoReserva.EN_CURSO)
                ? EstadoReserva.EN_CURSO
                : detalles.stream().anyMatch(d -> d.getEstadoDetalle() == EstadoReserva.CONFIRMADA)
                    ? EstadoReserva.CONFIRMADA
                    : EstadoReserva.FINALIZADA;
        reserva.setEstadoReserva(estado);
    }

    private List<DetalleReserva> visibles(List<DetalleReserva> rows) {
        var active = rows
            .stream()
            .filter(r -> r.getEstadoDetalle() != EstadoReserva.CANCELADA)
            .toList();
        return active.isEmpty() ? rows : active;
    }

    private Map<Long, Habitacion> bloquearHabitaciones(List<ReservaDetalleRequestDTO> lines) {
        if (
            lines.stream().map(ReservaDetalleRequestDTO::getIdHabitacion).distinct().count() != lines.size()
        ) throw new ReglaNegocioException("No se puede agregar la misma habitación dos veces");
        Map<Long, Habitacion> result = new LinkedHashMap<>();
        lines
            .stream()
            .map(ReservaDetalleRequestDTO::getIdHabitacion)
            .sorted()
            .forEach(id ->
                result.put(
                    id,
                    habitaciones
                        .bloquear(id)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Habitación no encontrada"))
                )
            );
        return result;
    }

    private void validarEstancias(
        List<ReservaDetalleRequestDTO> lines,
        Map<Long, Habitacion> rooms,
        Long idReserva,
        boolean soloPersonas
    ) {
        for (var line : lines) {
            Habitacion room = rooms.get(line.getIdHabitacion());
            if (!line.getFechaSalida().isAfter(line.getFechaIngreso())) throw new ReglaNegocioException(
                "La salida debe ser posterior al ingreso"
            );
            if (
                !soloPersonas && room.getEstadoHabitacion() != EstadoHabitacion.DISPONIBLE
            ) throw new ReglaNegocioException(
                "La habitación " + room.getNumeroCuarto() + " no está disponible para registrar reservas"
            );
            if (
                detalleReservaRepository.superpuestas(
                    line.getIdHabitacion(),
                    line.getFechaIngreso(),
                    line.getFechaSalida(),
                    idReserva
                ) >
                0
            ) throw new ReglaNegocioException(
                "La habitación " + room.getNumeroCuarto() + " ya tiene una reserva en esas fechas"
            );
            if (line.getHuespedes().size() > room.getCapacidadPersonas()) throw new ReglaNegocioException(
                "Se excedió la capacidad de la habitación " + room.getNumeroCuarto()
            );
            long sinCama = line
                .getHuespedes()
                .stream()
                .filter(h -> Boolean.FALSE.equals(h.getOcupaPlazaCama()))
                .count();
            if (sinCama > room.getMaxMenoresSinCama()) throw new ReglaNegocioException(
                "La habitación " +
                    room.getNumeroCuarto() +
                    " admite como máximo " +
                    room.getMaxMenoresSinCama() +
                    " menores sin plaza de cama propia"
            );
            if (line.getHuespedes().size() - sinCama > room.getPlazasCama()) throw new ReglaNegocioException(
                "Se excedieron las plazas de cama de la habitación " + room.getNumeroCuarto()
            );
            Set<String> documents = new HashSet<>();
            for (var guest : line.getHuespedes())
                if (
                    !documents.add(
                        guest.getPersona().getIdTipoDocumento() + ":" + guest.getPersona().getNumeroDocumento().trim()
                    )
                ) throw new ReglaNegocioException("No repitas un huésped dentro de la misma habitación");
        }
    }

    private DetalleReserva crearLinea(Reserva reserva, Habitacion room, ReservaDetalleRequestDTO line) {
        var row = new DetalleReserva();
        reserva.agregarDetalle(row);
        row.setHabitacion(room);
        row.setFechaIngreso(line.getFechaIngreso());
        row.setFechaSalida(line.getFechaSalida());
        row.setCostoEstadia(costo(room, line));
        row.setEstadoDetalle(EstadoReserva.CONFIRMADA);
        detalleReservaRepository.save(row);
        var charge = new Consumo();
        charge.setDetalleReserva(row);
        charge.setCliente(reserva.getCliente());
        charge.setUsuario(reserva.getUsuario());
        completarCargo(charge, row);
        consumos.save(charge);
        guardarOcupantes(row, line.getHuespedes());
        return row;
    }

    private void guardarOcupantes(DetalleReserva row, List<ReservaHuespedRegistroRequestDTO> guests) {
        guardarOcupantes(row, guests, Map.of());
    }

    private void guardarOcupantes(
        DetalleReserva row,
        List<ReservaHuespedRegistroRequestDTO> guests,
        Map<Long, Long> perfilesPermitidos
    ) {
        Map<String, ReservaHuesped> registrados = new LinkedHashMap<>();
        // Primero los adultos: el responsable ya debe existir en la misma habitación.
        var ordenados = guests
            .stream()
            .sorted(java.util.Comparator.comparing(guest -> guest.getResponsable() != null))
            .toList();
        for (ReservaHuespedRegistroRequestDTO guest : ordenados) {
            Perfil perfil;
            if (guest.getIdHuesped() != null) {
                Long idPerfil = perfilesPermitidos.get(guest.getIdHuesped());
                if (idPerfil == null) throw new ReglaNegocioException(
                    "El huésped a editar no pertenece a esta reserva"
                );
                // La ficha del cliente se actualiza una vez, usando los datos del titular.
                perfil = idPerfil.equals(row.getReserva().getCliente().getPerfil().getIdPerfil())
                    ? row.getReserva().getCliente().getPerfil()
                    : clientes.actualizarPersona(idPerfil, guest.getPersona());
            } else perfil = clientes.guardarPersona(guest.getPersona());
            Huesped huesped = huespedes
                .findByPersonaNaturalIdPerfil(perfil.getIdPerfil())
                .orElseGet(() -> {
                    Huesped nuevo = new Huesped();
                    nuevo.setPersonaNatural(naturales.findById(perfil.getIdPerfil()).orElseThrow());
                    return huespedes.save(nuevo);
                });
            var tipo = tiposOcupante
                .findById(guest.getIdTipoOcupante())
                .orElseThrow(() -> new RecursoNoEncontradoException("El tipo de ocupante no existe"));
            ReservaHuesped relacion = new ReservaHuesped();
            relacion.setId(new ReservaHuespedId(row.getIdDetalleReserva(), huesped.getIdHuesped()));
            relacion.setDetalleReserva(row);
            relacion.setHuesped(huesped);
            relacion.setTipoOcupante(tipo);
            relacion.setOcupaPlazaCama(guest.getOcupaPlazaCama());
            if (guest.getResponsable() != null) {
                var responsable = guest.getResponsable();
                ReservaHuesped adulto = registrados.get(
                    claveDocumento(responsable.getIdTipoDocumento(), responsable.getNumeroDocumento())
                );
                if (adulto == null) throw new ReglaNegocioException(
                    "El responsable debe alojarse en la misma habitación que el menor"
                );
                relacion.setIdHuespedResponsable(adulto.getHuesped().getIdHuesped());
                relacion.setParentescoResponsable(responsable.getParentesco());
                relacion.setResponsable(adulto);
            }
            ocupantes.saveAndFlush(relacion);
            registrados.put(
                claveDocumento(perfil.getTipoDocumento().getIdTipoDocumento(), perfil.getNumeroDocumento()),
                relacion
            );
        }
    }

    private String claveDocumento(Long tipo, String numero) {
        return tipo + ":" + numero.trim();
    }

    private ResponsableHuespedRequestDTO convertirResponsable(ReservaHuesped relacion) {
        if (relacion.getIdHuespedResponsable() == null) return null;
        Perfil perfil = relacion.getResponsable().getHuesped().getPersonaNatural().getPerfil();
        return new ResponsableHuespedRequestDTO(
            perfil.getTipoDocumento().getIdTipoDocumento(),
            perfil.getNumeroDocumento(),
            relacion.getParentescoResponsable()
        );
    }

    private Consumo cargo(DetalleReserva row) {
        return consumos
            .findByDetalleReservaIdDetalleReserva(row.getIdDetalleReserva())
            .stream()
            .filter(c -> c.getTipoConsumo() == TipoConsumo.HOSPEDAJE)
            .findFirst()
            .orElseThrow(() ->
                new ReglaNegocioException("La reserva no tiene cargo de hospedaje. Revisa sus datos antes de cobrar.")
            );
    }

    private void completarCargo(Consumo cargo, DetalleReserva row) {
        cargo.setTipoConsumo(TipoConsumo.HOSPEDAJE);
        cargo.setDescripcion("Hospedaje · habitación " + row.getHabitacion().getNumeroCuarto());
        cargo.setCantidad(BigDecimal.ONE);
        cargo.setValorUnitario(row.getCostoEstadia());
        cargo.setSubtotal(row.getCostoEstadia());
        cargo.setIgv(BigDecimal.ZERO);
        cargo.setTotal(row.getCostoEstadia());
        cargo.setEstadoCargo(EstadoCargo.PENDIENTE);
        cargo.setEstadoCobro(EstadoCobro.PENDIENTE);
    }

    static BigDecimal costo(Habitacion room, ReservaDetalleRequestDTO line) {
        long nights = Math.max(
            1,
            (Duration.between(line.getFechaIngreso(), line.getFechaSalida()).toSeconds() + 86399) / 86400
        );
        BigDecimal cost = room
            .getTipoHabitacion()
            .getPrecioBase()
            .multiply(BigDecimal.valueOf(nights))
            .setScale(2, RoundingMode.HALF_UP);
        if (cost.compareTo(new BigDecimal("99999999.99")) > 0) throw new ReglaNegocioException(
            "El importe excede el límite permitido"
        );
        return cost;
    }

    private Usuario obtenerUsuarioReserva() {
        return usuarios
            .findByUsername("operacion_local")
            .orElseThrow(() ->
                new RecursoNoEncontradoException("No existe el usuario responsable de registrar la reserva")
            );
    }

    private void validarSolicitud(ReservaRegistroRequestDTO request) {
        var errores = validator.validate(request);
        if (!errores.isEmpty()) throw new ConstraintViolationException(errores);
    }

    private static String referencia(DetalleReserva reserva) {
        return "R" + reserva.getReserva().getIdReserva();
    }

    private ReservaResumenResponseDTO convertirResponse(List<DetalleReserva> rows, BigDecimal vuelto) {
        DetalleReserva first = rows.getFirst();
        List<ReservaDetalleResponseDTO> lines = rows
            .stream()
            .map(r ->
                new ReservaDetalleResponseDTO(
                    r.getIdDetalleReserva(),
                    r.getHabitacion().getIdHabitacion(),
                    r.getHabitacion().getNumeroCuarto(),
                    r.getHabitacion().getTipoHabitacion().getNombreTipo(),
                    r.getHabitacion().getCapacidadPersonas(),
                    r.getHabitacion().getPlazasCama(),
                    r.getHabitacion().getMaxMenoresSinCama(),
                    r.getHabitacion().getTipoHabitacion().getPrecioBase(),
                    r.getFechaIngreso(),
                    r.getFechaSalida(),
                    r.getCostoEstadia(),
                    r.getEstadoDetalle().name(),
                    ocupantes
                        .findByDetalleReservaIdDetalleReserva(r.getIdDetalleReserva())
                        .stream()
                        .map(o ->
                            new ReservaHuespedRegistroRequestDTO(
                                clientes.datos(o.getHuesped().getPersonaNatural().getPerfil()),
                                o.getTipoOcupante().getIdTipoOcupante(),
                                convertirResponsable(o),
                                o.getHuesped().getIdHuesped(),
                                o.getOcupaPlazaCama()
                            )
                        )
                        .toList()
                )
            )
            .toList();
        var receipts = comprobanteService.recibos(rows);
        BigDecimal paid = comprobanteService.pagado(rows);
        BigDecimal total = total(rows);
        boolean canceled = rows.stream().allMatch(r -> r.getEstadoDetalle() == EstadoReserva.CANCELADA);
        String state = first.getReserva().getEstadoReserva().name();
        String charge = canceled
            ? "ANULADO"
            : paid.compareTo(total) >= 0 && total.signum() > 0
                ? "PAGADO"
                : paid.signum() > 0
                    ? "PARCIAL"
                    : "PENDIENTE";
        String ref = referencia(first);
        return new ReservaResumenResponseDTO(
            first.getReserva().getIdReserva(),
            ref,
            String.format("%07d", Long.parseLong(ref.substring(1))),
            new ClienteResumenResponseDTO(
                first.getReserva().getCliente().getIdCliente(),
                clientes.datos(first.getReserva().getCliente().getPerfil())
            ),
            lines,
            state,
            charge,
            total,
            paid,
            canceled ? BigDecimal.ZERO : total.subtract(paid).max(BigDecimal.ZERO),
            receipts.isEmpty()
                ? null
                : receipts.getFirst().getSerie() + "-" + receipts.getFirst().getNumeroComprobante(),
            vuelto,
            comprobanteService.devuelto(rows),
            extras(rows).stream().map(Consumo::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add),
            extras(rows)
                .stream()
                .map(c ->
                    new pe.com.rebolsysBackend.dto.ConsumoReservaResponseDTO(
                        c.getIdConsumo(),
                        c.getVenta() == null ? null : c.getVenta().getIdVenta(),
                        c.getDetalleReserva().getIdDetalleReserva(),
                        c.getDetalleReserva().getHabitacion().getNumeroCuarto(),
                        c.getDescripcion(),
                        c.getCantidad(),
                        c.getTotal(),
                        c.getEstadoCobro().name(),
                        c.getFechaConsumo()
                    )
                )
                .toList(),
            receipts
                .stream()
                .sorted(java.util.Comparator.comparing(pe.com.rebolsysBackend.entity.Comprobante::getIdComprobante))
                .map(c ->
                    new pe.com.rebolsysBackend.dto.ComprobanteReferenciaResponseDTO(
                        c.getIdComprobante(),
                        c.getSerie() + "-" + c.getNumeroComprobante(),
                        c.getEstadoComprobante().name(),
                        c.getTotalPagar()
                    )
                )
                .toList()
        );
    }

    private List<Consumo> extras(List<DetalleReserva> rows) {
        return rows
            .stream()
            .flatMap(r -> consumos.findByDetalleReservaIdDetalleReserva(r.getIdDetalleReserva()).stream())
            .filter(c -> c.getTipoConsumo() != TipoConsumo.HOSPEDAJE && c.getEstadoCargo() != EstadoCargo.ANULADO)
            .toList();
    }

    private BigDecimal total(List<DetalleReserva> rows) {
        return rows
            .stream()
            .map(DetalleReserva::getCostoEstadia)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .add(extras(rows).stream().map(Consumo::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static BigDecimal vuelto(BigDecimal received, BigDecimal amount) {
        return received == null || amount.signum() == 0
            ? BigDecimal.ZERO
            : received.subtract(amount).max(BigDecimal.ZERO);
    }

    private String firma(ReservaRegistroRequestDTO request) {
        return hash(json.writeValueAsString(request));
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo identificar la solicitud", ex);
        }
    }
}
