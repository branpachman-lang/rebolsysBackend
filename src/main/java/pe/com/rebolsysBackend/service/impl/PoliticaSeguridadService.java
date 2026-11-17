package pe.com.rebolsysBackend.service.impl;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.SistemaDTO.Politica;
import pe.com.rebolsysBackend.entity.ParametroSistema;
import pe.com.rebolsysBackend.repository.SistemaRepository;

@Service
@Transactional(readOnly = true)
public class PoliticaSeguridadService {
  private static final String INACTIVIDAD = "SEGURIDAD_MINUTOS_INACTIVIDAD";
  private static final String INTENTOS = "SEGURIDAD_INTENTOS_FALLIDOS";
  private final SistemaRepository datos;
  private final AccesosService accesos;
  private final int minutosPredeterminados;

  public PoliticaSeguridadService(SistemaRepository datos, AccesosService accesos,
      @Value("${server.servlet.session.timeout:30m}") Duration inactividad) {
    this.datos = datos;
    this.accesos = accesos;
    this.minutosPredeterminados = (int) Math.clamp(inactividad.toMinutes(), 1, 1440);
  }

  public Politica consultar() {
    return new Politica(minutosInactividad(), maximoIntentos());
  }

  public int minutosInactividad() {
    return leer(INACTIVIDAD, minutosPredeterminados, 1440);
  }

  public int maximoIntentos() {
    return leer(INTENTOS, 5, 5);
  }

  @Transactional
  public Politica guardar(Politica politica) {
    accesos.bloquearGerencia();
    escribir(INACTIVIDAD, politica.minutosInactividad(), "Tiempo máximo de inactividad de sesión en minutos");
    escribir(INTENTOS, politica.intentosFallidos(), "Intentos fallidos antes de bloquear la cuenta");
    return politica;
  }

  private int leer(String clave, int predeterminado, int maximo) {
    var parametro = datos.parametro(clave);
    if (parametro == null) return predeterminado;
    try {
      int valor = Integer.parseInt(parametro.getValor());
      return valor >= 1 && valor <= maximo ? valor : predeterminado;
    } catch (NumberFormatException ex) {
      return predeterminado;
    }
  }

  private void escribir(String clave, int valor, String descripcion) {
    var parametro = datos.parametro(clave);
    var texto = Integer.toString(valor);
    if (parametro == null) datos.registrar(new ParametroSistema(null, clave, texto, descripcion));
    else parametro.setValor(texto);
  }
}
