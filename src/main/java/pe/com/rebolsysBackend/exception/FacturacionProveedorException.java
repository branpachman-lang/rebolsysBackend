package pe.com.rebolsysBackend.exception;

public class FacturacionProveedorException extends RuntimeException {
  private final boolean incierto;
  public FacturacionProveedorException(String mensaje, boolean incierto) {
    super(mensaje);
    this.incierto=incierto;
  }
  public boolean isIncierto() { return incierto; }
}
