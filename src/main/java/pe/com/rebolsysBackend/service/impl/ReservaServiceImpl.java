package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.*;

import java.time.*;
import java.util.*;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
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
public class ReservaServiceImpl implements ReservaService {
    private final jakarta.persistence.EntityManager entityManager;
    private final ReservaRepository reservas;
    private final DetalleReservaRepository detalles;
    private final ReservaOcupanteRepository ocupantes;
    private final HabitacionRepository habitaciones;
    private final ClienteRepository clientes;
    private final PersonaNaturalRepository naturales;
    private final PersonaJuridicaRepository juridicas;
    private final ListaNegraRepository listaNegra;
    private final ClienteService personas;
    private final ReglasPropuesta reglas;
    private final CuentaReservaGestion cuenta;

    @Override
    @Transactional
    public ReservaRespuestaDTO registrar(ReservaRegistroRequestDTO r) {
        reglas.validar(r);
        var usuario = reglas.bloquearUsuario(r.idUsuario());
        String firma = reglas.firma(r);
        var anterior = reservas.findByClaveRegistro(r.claveRegistro());
        if (anterior.isPresent()) {
            exigir(firma.equals(anterior.get().getFirmaSolicitud()), "La clave de registro ya corresponde a otra solicitud");
            return respuesta(anterior.get());
        }
        var titular = cliente(r.idCliente());
        var ids = r.detalles().stream().map(ReservaDetalleRegistroDTO::idHabitacion).sorted().toList();
        exigir(new HashSet<>(ids).size() == ids.size(), "Cada habitación debe aparecer una sola vez en la reserva");
        Map<Long, Habitacion> bloqueadas = new HashMap<>();
        for (Long id : ids) {
            var h = habitaciones.bloquear(id).orElseThrow(() -> new RecursoNoEncontradoException("La habitación no existe"));
            entityManager.refresh(h);
            bloqueadas.put(id, h);
        }
        // Bloquear también clientes en orden: dos operadores no pueden alojar a la misma persona a la
        // vez.
        var clientesIds = r.detalles().stream()
                .flatMap(d -> d.ocupantes().stream())
                .map(ReservaOcupanteRegistroDTO::idCliente)
                .distinct()
                .sorted()
                .toList();
        for (Long id : clientesIds)
            clientes.bloquear(id).orElseThrow(() -> new RecursoNoEncontradoException("El ocupante no existe"));
        var reserva = new Reserva();
        reserva.setCliente(titular);
        reserva.setUsuario(usuario);
        reserva.setClaveRegistro(r.claveRegistro());
        reserva.setFirmaSolicitud(firma);
        Set<Long> enSolicitud = new HashSet<>();
        for (var s : r.detalles()) {
            exigir(s.fechaSalida().isAfter(s.fechaIngreso()), "La salida debe ser posterior al ingreso");
            validarTitular(titular, s.fechaIngreso().toLocalDate());
            var h = bloqueadas.get(s.idHabitacion());
            exigir(h.getEstadoHabitacion() == EstadoHabitacion.DISPONIBLE, "La habitación no está disponible físicamente");
            exigir(detalles.superpuestas(h.getIdHabitacion(), s.fechaIngreso(), s.fechaSalida(), null) == 0, "La habitación ya tiene una reserva en ese periodo o una estancia en curso");
            validarOcupantes(s, h);
            for (var o : s.ocupantes())
                exigir(enSolicitud.add(o.idCliente()), "El ocupante aparece en más de una habitación de la solicitud");
            var d = new DetalleReserva();
            d.setHabitacion(h);
            d.setFechaIngreso(s.fechaIngreso());
            d.setFechaSalida(s.fechaSalida());
            d.setCostoEstadia(s.costoEstadia());
            d.setEstadoDetalle(EstadoReserva.CONFIRMADA);
            reserva.agregarDetalle(d);
        }
        reservas.saveAndFlush(reserva);
        for (int i = 0; i < r.detalles().size(); i++)
            guardarOcupantes(reserva.getDetalles().get(i), r.detalles().get(i).ocupantes());
        return respuesta(reserva);
    }

    private Cliente cliente(Long id) {
        var c = clientes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El cliente no existe"));
        exigir(!listaNegra.existsByPerfilIdPerfil(c.getPerfil().getIdPerfil()), "La persona está registrada en la lista negra");
        return c;
    }

    private void validarTitular(Cliente c, LocalDate ingreso) {
        if (naturales.existsById(c.getPerfil().getIdPerfil()))
            exigir(edad(c, ingreso) >= 18, "El titular de la reserva debe ser mayor de edad");
        else
            exigir(
                    juridicas.existsById(c.getPerfil().getIdPerfil()),
                    "El titular debe tener identidad registrada");
    }

    private int edad(Cliente c, LocalDate fecha) {
        var n = naturales.findById(c.getPerfil().getIdPerfil()).orElseThrow(() -> new ReglaNegocioException("Los ocupantes deben ser personas naturales"));
        exigir(
                n.getFechaNacimiento() != null && !n.getFechaNacimiento().isAfter(fecha),
                "Se requiere una fecha de nacimiento válida del ocupante");
        return Period.between(n.getFechaNacimiento(), fecha).getYears();
    }

    private void validarOcupantes(ReservaDetalleRegistroDTO d, Habitacion h) {
        validarOcupantes(d, h, null);
    }

    private void validarOcupantes(ReservaDetalleRegistroDTO d, Habitacion h, Long excluir) {
        Map<Long, Integer> edades = new HashMap<>();
        for (var o : d.ocupantes()) {
            exigir(
                    !edades.containsKey(o.idCliente()),
                    "Un cliente no puede ocupar dos plazas en la misma habitación");
            edades.put(o.idCliente(), edad(cliente(o.idCliente()), d.fechaIngreso().toLocalDate()));
            exigir(
                    ocupantes.superposicionesCliente(
                            o.idCliente(), d.fechaIngreso(), d.fechaSalida(), excluir)
                            == 0,
                    "El ocupante ya está registrado en otra estancia durante ese periodo");
        }
        exigir(
                d.ocupantes().size() <= h.getCapacidadPersonas(),
                "Se supera la capacidad de personas de la habitación");
        long camas = d.ocupantes().stream().filter(o -> o.ocupaPlazaCama()).count();
        exigir(camas <= h.getPlazasCama(), "Se superan las plazas de cama de la habitación");
        exigir(
                d.ocupantes().size() - camas <= h.getMaxMenoresSinCama(),
                "Se supera el límite de menores sin cama");
        for (var o : d.ocupantes()) {
            int edad = edades.get(o.idCliente());
            if (!o.ocupaPlazaCama())
                exigir(edad < 7, "Solo un menor de siete años puede alojarse sin plaza de cama");
            if (edad < 18) {
                exigir(
                        o.idClienteResponsable() != null
                                && !o.idCliente().equals(o.idClienteResponsable())
                                && o.parentescoResponsable() != null,
                        "Todo menor necesita un responsable y parentesco");
                exigir(
                        edades.getOrDefault(o.idClienteResponsable(), -1) >= 18,
                        "El responsable debe ser adulto y ocupar la misma habitación");
            } else
                exigir(
                        o.idClienteResponsable() == null && o.parentescoResponsable() == null,
                        "Un ocupante adulto no necesita responsable");
        }
    }

    private void guardarOcupantes(DetalleReserva d, List<ReservaOcupanteRegistroDTO> solicitudes) {
        Map<Long, ReservaOcupante> porCliente = new HashMap<>();
        // Insertar responsables primero; el CHECK exige guardar completos los vínculos del menor.
        for (var r :
                solicitudes.stream()
                        .sorted(Comparator.comparing(o -> o.idClienteResponsable() != null))
                        .toList()) {
            var o = new ReservaOcupante();
            o.setDetalleReserva(d);
            o.setCliente(cliente(r.idCliente()));
            o.setOcupaPlazaCama(r.ocupaPlazaCama() ? 1 : 0);
            if (r.idClienteResponsable() != null) {
                o.setIdOcupanteResponsable(porCliente.get(r.idClienteResponsable()).getIdReservaOcupante());
                o.setParentescoResponsable(r.parentescoResponsable());
            }
            ocupantes.saveAndFlush(o);
            porCliente.put(r.idCliente(), o);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaRespuestaDTO obtener(Long id) {
        return respuesta(
                reservas
                        .findById(id)
                        .orElseThrow(() -> new RecursoNoEncontradoException("La reserva no existe")));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponseDTO<ReservaRespuestaDTO> listar(int pagina, int tamanio) {
        var p = reservas.findAll(PageRequest.of(pagina, tamanio, Sort.by("idReserva").descending()));
        return new PaginaResponseDTO<>(
                p.getContent().stream().map(this::respuesta).toList(),
                pagina,
                tamanio,
                p.getTotalElements(),
                p.getTotalPages(),
                p.isLast());
    }

    @Override
    @Transactional
    public ReservaDetalleRespuestaDTO cambiarEstado(Long id, String accion, OperacionUsuarioDTO r) {
        reglas.validar(r);
        reglas.usuario(r.idUsuario());
        var actual =
                detalles
                        .findById(id)
                        .orElseThrow(() -> new RecursoNoEncontradoException("La estancia no existe"));
        reservas.bloquear(actual.getReserva().getIdReserva()).orElseThrow();
        var h = habitaciones.bloquear(actual.getHabitacion().getIdHabitacion()).orElseThrow();
        entityManager.refresh(h);
        var d = detalles.bloquear(id).orElseThrow();
        entityManager.refresh(d);
        var ahora = ahora();
        switch (accion) {
            case "ingreso" -> {
                if (d.getEstadoDetalle() == EstadoReserva.EN_CURSO) return respuestaDetalle(d);
                exigir(
                        d.getEstadoDetalle() == EstadoReserva.CONFIRMADA,
                        "Solo puede ingresar una estancia confirmada");
                var registrados =
                        ocupantes.findByDetalleReservaIdDetalleReservaOrderByIdReservaOcupante(id);
                exigir(!registrados.isEmpty(), "Completa los ocupantes antes del ingreso");
                Map<Long, Long> clientesPorOcupante = new HashMap<>();
                registrados.forEach(
                        o -> clientesPorOcupante.put(o.getIdReservaOcupante(), o.getCliente().getIdCliente()));
                var lista = registrados.stream().map(o -> new ReservaOcupanteRegistroDTO(o.getCliente().getIdCliente(),
                                                        clientesPorOcupante.get(o.getIdOcupanteResponsable()),
                                                        o.getParentescoResponsable(),
                                                        o.getOcupaPlazaCama() == 1)).toList();
                validarOcupantes(
                        new ReservaDetalleRegistroDTO(
                                h.getIdHabitacion(), ahora, d.getFechaSalida(), d.getCostoEstadia(), lista),
                        h,
                        id);
                exigir(
                        !ahora.isBefore(d.getFechaIngreso()) && ahora.isBefore(d.getFechaSalida()),
                        "El ingreso debe estar dentro del periodo reservado");
                exigir(
                        h.getEstadoHabitacion() == EstadoHabitacion.DISPONIBLE,
                        "La habitación debe estar disponible");
                exigir(
                        detalles.superpuestas(h.getIdHabitacion(), ahora, d.getFechaSalida(), id) == 0,
                        "Otra estancia impide el ingreso");
                d.setFechaIngresoReal(ahora);
                d.setEstadoDetalle(EstadoReserva.EN_CURSO);
                h.setEstadoHabitacion(EstadoHabitacion.OCUPADO);
            }
            case "salida" -> {
                if (d.getEstadoDetalle() == EstadoReserva.FINALIZADA) return respuestaDetalle(d);
                exigir(
                        d.getEstadoDetalle() == EstadoReserva.EN_CURSO,
                        "Solo puede salir una estancia en curso");
                cuenta.validarSalida(d.getReserva());
                d.setFechaSalidaReal(ahora);
                d.setEstadoDetalle(EstadoReserva.FINALIZADA);
                h.setEstadoHabitacion(EstadoHabitacion.LIMPIEZA);
            }
            case "cancelacion" -> {
                if (d.getEstadoDetalle() == EstadoReserva.CANCELADA) return respuestaDetalle(d);
                exigir(
                        d.getEstadoDetalle() == EstadoReserva.CONFIRMADA,
                        "Solo puede cancelarse una estancia sin ingreso");
                cuenta.cancelarDetalle(d);
                d.setEstadoDetalle(EstadoReserva.CANCELADA);
            }
            default -> throw new ReglaNegocioException("La acción solicitada no es válida");
        }
        detalles.flush();
        return respuestaDetalle(d);
    }

    @Override
    @Transactional
    public ReservaDetalleRespuestaDTO guardarOcupantes(Long id, ReservaOcupantesGuardarDTO r) {
        reglas.validar(r);
        reglas.usuario(r.idUsuario());
        var previo =
                detalles
                        .findById(id)
                        .orElseThrow(() -> new RecursoNoEncontradoException("La estancia no existe"));
        reservas.bloquear(previo.getReserva().getIdReserva()).orElseThrow();
        var h = habitaciones.bloquear(previo.getHabitacion().getIdHabitacion()).orElseThrow();
        entityManager.refresh(h);
        var d = detalles.bloquear(id).orElseThrow();
        entityManager.refresh(d);
        exigir(
                d.getEstadoDetalle() == EstadoReserva.CONFIRMADA,
                "Los ocupantes solo se pueden completar o corregir antes del ingreso");
        for (var clienteId :
                r.ocupantes().stream()
                        .map(ReservaOcupanteRegistroDTO::idCliente)
                        .distinct()
                        .sorted()
                        .toList())
            clientes
                    .bloquear(clienteId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("El ocupante no existe"));
        validarOcupantes(
                new ReservaDetalleRegistroDTO(
                        h.getIdHabitacion(),
                        d.getFechaIngreso(),
                        d.getFechaSalida(),
                        d.getCostoEstadia(),
                        r.ocupantes()),
                h,
                id);
        ocupantes.eliminarDeDetalle(id);
        ocupantes.flush();
        guardarOcupantes(d, r.ocupantes());
        return respuestaDetalle(d);
    }

    private ReservaRespuestaDTO respuesta(Reserva r) {
        return new ReservaRespuestaDTO(
                r.getIdReserva(),
                r.getCliente().getIdCliente(),
                r.getUsuario().getIdUsuario(),
                personas.nombre(r.getCliente().getPerfil()),
                r.getFechaRegistro(),
                r.getClaveRegistro(),
                r.getDetalles().stream().map(this::respuestaDetalle).toList());
    }

    private ReservaDetalleRespuestaDTO respuestaDetalle(DetalleReserva d) {
        return new ReservaDetalleRespuestaDTO(
                d.getIdDetalleReserva(),
                d.getHabitacion().getIdHabitacion(),
                d.getHabitacion().getNumeroCuarto(),
                d.getFechaIngreso(),
                d.getFechaSalida(),
                d.getFechaIngresoReal(),
                d.getFechaSalidaReal(),
                d.getCostoEstadia(),
                d.getEstadoDetalle(),
                ocupantes
                        .findByDetalleReservaIdDetalleReservaOrderByIdReservaOcupante(d.getIdDetalleReserva())
                        .stream()
                        .map(
                                o ->
                                        new ReservaOcupanteRespuestaDTO(
                                                o.getIdReservaOcupante(),
                                                o.getCliente().getIdCliente(),
                                                personas.nombre(o.getCliente().getPerfil()),
                                                o.getIdOcupanteResponsable(),
                                                o.getParentescoResponsable(),
                                                o.getOcupaPlazaCama() == 1))
                        .toList());
    }
}
