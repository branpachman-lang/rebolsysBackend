package pe.com.rebolsysBackend.service.service;

import java.util.List;
import org.springframework.data.domain.Pageable;
import pe.com.rebolsysBackend.dto.LimpiezaDTO.*;
import pe.com.rebolsysBackend.dto.PaginaResponseDTO;

public interface LimpiezaService {
  Inicio inicio();
  List<HabitacionResumen> habitaciones(FiltroHabitaciones filtro);
  HabitacionDetalle habitacion(Long id);
  Borrador iniciar(Long idHabitacion);
  void cancelarBorrador(Long idInforme);
  InformeDetalle enviar(Long idInforme, EnviarInforme datos);
  PaginaResponseDTO<InformeResumen> informes(FiltroInformes filtro, Pageable pagina);
  InformeDetalle informe(Long id);
  InformeDetalle editarInforme(Long id, EditarInforme datos);
  void eliminarInforme(Long id);
  InformeDetalle aprobar(Long id);
  PaginaResponseDTO<TicketResumen> tickets(FiltroTickets filtro, Pageable pagina);
  TicketDetalle ticket(Long id);
  TicketDetalle crearTicket(Long idInforme, Long idDetalle, CrearTicket datos);
  TicketDetalle editarTicket(Long id, DatosTicket datos);
  void eliminarTicket(Long id);
}
