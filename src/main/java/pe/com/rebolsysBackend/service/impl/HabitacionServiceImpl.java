package pe.com.rebolsysBackend.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.HabitacionEstadoResponseDTO;
import pe.com.rebolsysBackend.dto.HabitacionOcupacionRequestDTO;
import pe.com.rebolsysBackend.dto.HabitacionOcupacionResponseDTO;
import pe.com.rebolsysBackend.dto.HabitacionReservaResponseDTO;
import pe.com.rebolsysBackend.entity.DetalleReserva;
import pe.com.rebolsysBackend.entity.Habitacion;
import pe.com.rebolsysBackend.enums.EstadoHabitacion;
import pe.com.rebolsysBackend.exception.RecursoNoEncontradoException;
import pe.com.rebolsysBackend.exception.ReglaNegocioException;
import pe.com.rebolsysBackend.repository.DetalleReservaRepository;
import pe.com.rebolsysBackend.repository.HabitacionRepository;
import pe.com.rebolsysBackend.service.service.ClienteService;
import pe.com.rebolsysBackend.service.service.HabitacionService;

@Service
@RequiredArgsConstructor
public class HabitacionServiceImpl implements HabitacionService {

    private final HabitacionRepository habitaciones;
    private final DetalleReservaRepository reservas;
    private final ClienteService clientes;
    private final EntityManager em;
    private final Validator validator;

    @Override
    @Transactional(readOnly = true)
    public List<HabitacionEstadoResponseDTO> estado(LocalDate desde, LocalDate hasta) {
        if (!hasta.isAfter(desde)) throw new ReglaNegocioException(
            "La fecha hasta debe ser posterior a la fecha desde"
        );
        LocalDateTime ingreso = desde.atStartOfDay(),
            salida = hasta.atStartOfDay();
        var ocupadas = reservas.buscarEnPeriodo(ingreso, salida);
        return habitaciones
            .findAll()
            .stream()
            .sorted(Comparator.comparing(Habitacion::getNumeroCuarto))
            .map(h -> {
                var bloques = ocupadas
                    .stream()
                    .filter(r -> r.getHabitacion().getIdHabitacion().equals(h.getIdHabitacion()))
                    .map(r ->
                        new HabitacionReservaResponseDTO(
                            referencia(r),
                            clientes.nombre(r.getReserva().getCliente().getPerfil()),
                            r.getFechaIngreso(),
                            r.getFechaSalida(),
                            r.getEstadoDetalle().name()
                        )
                    )
                    .toList();
                return new HabitacionEstadoResponseDTO(
                    h.getIdHabitacion(),
                    h.getNumeroCuarto(),
                    h.getTipoHabitacion().getNombreTipo(),
                    h.getCapacidadPersonas(),
                    h.getPlazasCama(),
                    h.getMaxMenoresSinCama(),
                    h.getTipoHabitacion().getPrecioBase(),
                    h.getEstadoHabitacion().name(),
                    h.getEstadoHabitacion() == EstadoHabitacion.DISPONIBLE && bloques.isEmpty(),
                    bloques
                );
            })
            .toList();
    }

    private String referencia(DetalleReserva reserva) {
        return "R" + reserva.getReserva().getIdReserva();
    }

    @Override
    @Transactional
    public HabitacionOcupacionResponseDTO actualizarOcupacion(Long id, HabitacionOcupacionRequestDTO request) {
        var errores = validator.validate(request);
        if (!errores.isEmpty()) throw new ConstraintViolationException(errores);
        var habitacion = habitaciones
            .bloquear(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("La habitación no existe"));
        habitacion.setCapacidadPersonas(request.getCapacidadPersonas());
        habitacion.setPlazasCama(request.getPlazasCama());
        habitacion.setMaxMenoresSinCama(request.getMaxMenoresSinCama());
        // Oracle comprueba también las estancias activas para no invalidarlas.
        em.flush();
        return new HabitacionOcupacionResponseDTO(
            habitacion.getIdHabitacion(),
            habitacion.getCapacidadPersonas(),
            habitacion.getPlazasCama(),
            habitacion.getMaxMenoresSinCama()
        );
    }
}
