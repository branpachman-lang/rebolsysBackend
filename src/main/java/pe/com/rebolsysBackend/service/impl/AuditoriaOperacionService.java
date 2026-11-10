package pe.com.rebolsysBackend.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.rebolsysBackend.dto.SesionActualDTO;
import pe.com.rebolsysBackend.entity.*;
import pe.com.rebolsysBackend.enums.AccionAuditoria;

@Service
@RequiredArgsConstructor
public class AuditoriaOperacionService {
    private final EntityManager em;

    @Transactional
    public void registrar(SesionActualDTO sesion, String tabla, Long id, String metodo, String ruta, String ip) {
        var a = new AuditoriaGeneral();
        a.setNombreTabla(tabla);
        a.setIdRegistro(id);
        a.setUsuario(em.getReference(Usuario.class, sesion.idUsuario()));
        boolean alta = "POST".equals(metodo) && (
            ruta.matches("/api/v1/[^/]+(?:/confirmadas)?")
                || ruta.matches("/api/v1/seguridad/(empleados|usuarios|roles)")
                || ruta.matches("/api/v1/caja/turnos/\\d+/movimientos")
                || ruta.matches("/api/v1/limpieza/(habitaciones/\\d+/iniciar|informes/\\d+/items/\\d+/ticket)"));
        a.setAccion("DELETE".equals(metodo) ? AccionAuditoria.DELETE : alta ? AccionAuditoria.INSERT : AccionAuditoria.UPDATE);
        a.setFechaHora(java.time.LocalDateTime.now(java.time.ZoneId.of("America/Lima")));
        a.setDatosNuevos(metodo + " " + ruta);
        a.setIpOrigen(ip);
        em.persist(a);
        em.flush();
    }
}
