package pe.com.rebolsysBackend.dto;

import java.util.List;

public record SesionActualDTO(Long idUsuario, String usuario, String modo,
    Long idTurno, Long idFormaPagoEfectivo, boolean requiereCambio,
    boolean gerente, List<String> roles, List<String> permisos) {
  public SesionActualDTO(Long idUsuario, String usuario, String modo, Long idTurno, Long idFormaPagoEfectivo) {
    this(idUsuario, usuario, modo, idTurno, idFormaPagoEfectivo, false, false, List.of(), List.of());
  }

  public boolean tiene(String permiso) {
    return !requiereCambio && (gerente || "PRUEBA".equals(modo) || permisos.contains(permiso));
  }
}
