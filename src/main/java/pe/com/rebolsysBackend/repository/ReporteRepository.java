package pe.com.rebolsysBackend.repository;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import pe.com.rebolsysBackend.dto.ReportesDTO.*;

@Repository
@RequiredArgsConstructor
public class ReporteRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public Pagina<Incidencia> incidencias(LocalDateTime desde, LocalDateTime hasta, String habitacion,
      String estado, int pagina, int tamanio) {
    return paginar("""
        select i.idInformeRevision, h.numeroCuarto, i.fechaHora, i.estadoInforme, m.nombreItem,
               d.estadoEncontrado, d.observacion, t.idTicket, t.estadoTicket
        from detalle_informe d
        join informe_revision i on i.idInformeRevision = d.idInforme
        join habitacion h on h.idHabitacion = d.idHabitacion
        join item m on m.idItem = d.idItem
        left join ticket_mantenimiento t on t.idDetalleInforme = d.idDetalleInforme
        where i.estadoInforme <> 'BORRADOR' and d.estadoEncontrado <> 'OPTIMO'
          and (:desde is null or i.fechaHora >= :desde)
          and (:hasta is null or i.fechaHora < :hasta)
          and (:cuarto is null or lower(h.numeroCuarto) like :cuarto escape '!')
          and (:estado is null or i.estadoInforme = :estado)
        order by i.fechaHora desc, d.idDetalleInforme desc
        """, fechas(desde, hasta).addValue("cuarto", habitacion).addValue("estado", estado), pagina, tamanio,
        (rs, n) -> new Incidencia(rs.getLong(1), rs.getString(2), rs.getObject(3, LocalDateTime.class),
            rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7),
            rs.getObject(8, Long.class), rs.getString(9)));
  }

  public List<Importe> financiero(LocalDateTime desde, LocalDateTime hasta) {
    return jdbc.query("""
        select concepto, medio, sum(monto)
        from (
          select case when c.idReserva is null then 'VENTAS' else 'RESERVAS' end concepto, f.nombrePago medio, p.monto
          from pago p join comprobante c on c.idComprobante = p.idComprobante
          join forma_pago f on f.idFormaPago = p.idFormaPago
          where p.verificado = 1 and (:desde is null or p.fechaPago >= :desde)
            and (:hasta is null or p.fechaPago < :hasta)
          union all
          select case when t.signo = -1 then 'EGRESOS' else 'INGRESOS_MANUALES' end, 'EFECTIVO', f.monto
          from flujo_caja f join tipo_movimiento_caja t on t.idTipoMovimiento = f.idTipoMovimiento
          where f.idPago is null and (t.signo = -1 or (t.signo = 1 and t.nombreMovimiento = 'INGRESO_MANUAL'))
            and (:desde is null or f.fechaHora >= :desde) and (:hasta is null or f.fechaHora < :hasta)
        ) movimientos
        group by concepto, medio order by concepto, medio
        """, fechas(desde, hasta), (rs, n) -> new Importe(rs.getString(1), rs.getString(2), rs.getBigDecimal(3)));
  }

  public Pagina<Cierre> cierres(LocalDateTime desde, LocalDateTime hasta, String usuario, int pagina, int tamanio) {
    return paginar("""
        select t.idTurno, u.username, t.fechaApertura, t.fechaCierre, f.nombrePago,
               a.montoCalculado, a.montoDeclarado, a.diferencia
        from turno_caja t join usuario u on u.idUsuario = t.idUsuario
        left join arqueo_caja a on a.idTurno = t.idTurno
        left join forma_pago f on f.idFormaPago = a.idFormaPago
        where t.estadoSesion = 0 and t.fechaCierre is not null
          and (:desde is null or t.fechaCierre >= :desde)
          and (:hasta is null or t.fechaCierre < :hasta)
          and (:usuario is null or lower(u.username) like :usuario escape '!')
        order by t.fechaCierre desc, t.idTurno desc, a.idFormaPago
        """, fechas(desde, hasta).addValue("usuario", usuario), pagina, tamanio,
        (rs, n) -> new Cierre(rs.getLong(1), rs.getString(2), rs.getObject(3, LocalDateTime.class),
            rs.getObject(4, LocalDateTime.class), rs.getString(5), rs.getBigDecimal(6),
            rs.getBigDecimal(7), rs.getBigDecimal(8)));
  }

  public Pagina<Existencia> productos(String buscar, boolean bajo, int pagina, int tamanio) {
    return paginar("""
        select p.idProducto, p.nombreProducto, c.nombreCategoria, p.estadoProducto, p.stock, p.stockMinimo
        from producto p join categoria c on c.idCategoria = p.idCategoria
        where (:buscar is null or lower(p.nombreProducto) like :buscar escape '!'
            or lower(c.nombreCategoria) like :buscar escape '!')
          and (:bajo = 0 or (p.estadoProducto = 'ACTIVO' and p.stock <= p.stockMinimo))
        order by p.nombreProducto, p.idProducto
        """, new MapSqlParameterSource("buscar", buscar).addValue("bajo", bajo ? 1 : 0), pagina, tamanio,
        (rs, n) -> new Existencia(rs.getLong(1), rs.getString(2), rs.getString(3),
            rs.getString(4), rs.getInt(5), rs.getInt(6)));
  }

  public Pagina<Mueble> mobiliario(String buscar, String habitacion, int pagina, int tamanio) {
    return paginar("""
        select i.idItem, i.nombreItem, t.nombreItem, h.numeroCuarto, i.estadoItem, i.condicionItem
        from item i join tipo_item t on t.idTipoItem = i.idTipoItem
        left join habitacion h on h.idHabitacion = i.idHabitacion
        where (:buscar is null or lower(i.nombreItem) like :buscar escape '!'
            or lower(t.nombreItem) like :buscar escape '!')
          and (:cuarto is null or lower(h.numeroCuarto) like :cuarto escape '!')
        order by h.numeroCuarto nulls last, i.nombreItem, i.idItem
        """, new MapSqlParameterSource("buscar", buscar).addValue("cuarto", habitacion), pagina, tamanio,
        (rs, n) -> new Mueble(rs.getLong(1), rs.getString(2), rs.getString(3),
            rs.getString(4), rs.getString(5), rs.getString(6)));
  }

  private static MapSqlParameterSource fechas(LocalDateTime desde, LocalDateTime hasta) {
    return new MapSqlParameterSource().addValue("desde", desde, java.sql.Types.TIMESTAMP)
        .addValue("hasta", hasta, java.sql.Types.TIMESTAMP);
  }

  private <T> Pagina<T> paginar(String sql, MapSqlParameterSource params, int pagina, int tamanio, RowMapper<T> mapper) {
    params.addValue("offset", (long) pagina * tamanio).addValue("limite", tamanio + 1);
    var filas = jdbc.query(sql + " offset :offset rows fetch next :limite rows only", params, mapper);
    boolean siguiente = filas.size() > tamanio;
    return new Pagina<>(siguiente ? List.copyOf(filas.subList(0, tamanio)) : filas, pagina, siguiente);
  }
}
