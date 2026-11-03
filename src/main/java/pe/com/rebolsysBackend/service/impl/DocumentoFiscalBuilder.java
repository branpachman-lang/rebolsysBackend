package pe.com.rebolsysBackend.service.impl;

import static pe.com.rebolsysBackend.service.impl.ReglasPropuesta.exigir;

import java.math.*;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.com.rebolsysBackend.config.FacturacionApisPeruProperties;
import pe.com.rebolsysBackend.dto.EmisionElectronicaRequestDTO;
import pe.com.rebolsysBackend.entity.*;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class DocumentoFiscalBuilder {
  private static final DateTimeFormatter FECHA_SUNAT=DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssXXX",Locale.ROOT);
  private final FacturacionApisPeruProperties config;
  private final ObjectMapper json;

  public record Documento(String solicitud,BigDecimal subtotal,BigDecimal igv,BigDecimal total) {}

  public Documento construir(Comprobante c,List<ComprobanteDetalle> lineas,BigDecimal saldo,
      EmisionElectronicaRequestDTO request) {
    exigir(!lineas.isEmpty(),"El comprobante no tiene líneas");
    exigir(c.getReceptorNombre()!=null && !c.getReceptorNombre().isBlank(),"Faltan los datos fijados del receptor");
    exigir(c.getReceptorTipoDoc()!=null && c.getReceptorTipoDoc().matches("[0-9A-Z]"),
        "El receptor necesita un tipo de documento del catálogo SUNAT");
    String tipo="FACTURA".equals(c.getTipoComprobante().getCodigo())?"01":"03";
    if ("01".equals(tipo)) {
      exigir("6".equals(c.getReceptorTipoDoc()) && c.getReceptorNumeroDoc().matches("[0-9]{11}"),
          "La factura necesita un receptor con RUC");
      exigir(request.direccionReceptor()!=null && !request.direccionReceptor().isBlank(),
          "La factura necesita la dirección fiscal del receptor");
    }
    var impuestos=new HashMap<Integer,EmisionElectronicaRequestDTO.TratamientoLinea>();
    var numeros=lineas.stream().map(ComprobanteDetalle::getNumeroLinea).collect(java.util.stream.Collectors.toSet());
    for (var t:request.tratamientos()) {
      exigir(numeros.contains(t.numeroLinea()),"El tratamiento corresponde a una línea inexistente");
      exigir(impuestos.put(t.numeroLinea(),t)==null,"Hay un tratamiento repetido para la misma línea");
      exigir("10".equals(t.afectacion()) ? t.tasaIgv().signum()>0 : t.tasaIgv().signum()==0,
          "Una línea gravada requiere tasa positiva; una exonerada o inafecta requiere tasa cero");
    }
    var details=new ArrayList<Map<String,Object>>();
    var totales=new HashMap<String,BigDecimal>();
    BigDecimal neto=BigDecimal.ZERO, igv=BigDecimal.ZERO, total=BigDecimal.ZERO;
    for (var l:lineas) {
      exigir(l.getCantidad().signum()>0,"La cantidad de una línea debe ser positiva");
      var tratamiento=impuestos.get(l.getNumeroLinea());
      String afectacion=tratamiento==null?"10":tratamiento.afectacion();
      BigDecimal tasa=tratamiento==null?request.tasaIgv():tratamiento.tasaIgv();
      BigDecimal bruto=l.getValorUnitario().multiply(l.getCantidad()).add(l.getIgv()).setScale(2,RoundingMode.HALF_UP);
      BigDecimal base="10".equals(afectacion)
          ? bruto.divide(BigDecimal.ONE.add(tasa.movePointLeft(2)),2,RoundingMode.HALF_UP) : bruto;
      BigDecimal impuesto=bruto.subtract(base);
      var d=new LinkedHashMap<String,Object>();
      d.put("unidad",l.getDetalleVenta()==null?"ZZ":"NIU");
      d.put("cantidad",l.getCantidad());
      d.put("codProducto","L"+l.getNumeroLinea());
      d.put("descripcion",l.getDescripcion());
      d.put("mtoValorUnitario",base.divide(l.getCantidad(),10,RoundingMode.HALF_UP));
      d.put("mtoPrecioUnitario",bruto.divide(l.getCantidad(),10,RoundingMode.HALF_UP));
      d.put("mtoValorVenta",base);
      d.put("mtoBaseIgv",base);
      d.put("porcentajeIgv",tasa);
      d.put("igv",impuesto);
      d.put("totalImpuestos",impuesto);
      d.put("tipAfeIgv",afectacion);
      details.add(d);
      neto=neto.add(base); igv=igv.add(impuesto); total=total.add(bruto);
      totales.merge(afectacion,base,BigDecimal::add);
    }
    exigir(total.compareTo(c.getTotalPagar())==0,"Las líneas no coinciden con el total de la cuenta");
    exigir(total.signum()>0,"El total del comprobante debe ser positivo");
    exigir(saldo.signum()>=0 && saldo.compareTo(total)<=0,"El saldo de la cuenta es inconsistente");
    var body=new LinkedHashMap<String,Object>();
    body.put("ublVersion","2.1"); body.put("tipoOperacion","0101"); body.put("tipoDoc",tipo);
    body.put("serie",c.getSerie()); body.put("correlativo",c.getNumeroComprobante());
    body.put("fechaEmision",c.getFechaEmision().atOffset(ZoneOffset.ofHours(-5)).format(FECHA_SUNAT));
    body.put("tipoMoneda","PEN");
    if (saldo.signum()>0) {
      exigir(!request.cuotas().isEmpty(),"Una cuenta con saldo requiere cuotas para declarar la emisión al crédito");
      BigDecimal suma=request.cuotas().stream().map(EmisionElectronicaRequestDTO.Cuota::monto).reduce(BigDecimal.ZERO,BigDecimal::add);
      exigir(suma.compareTo(saldo)==0,"Las cuotas deben sumar exactamente el saldo pendiente");
      body.put("formaPago",Map.of("moneda","PEN","tipo","Credito","monto",saldo));
      body.put("cuotas",request.cuotas().stream().map(q->Map.of("moneda","PEN","monto",q.monto(),
          "fechaPago",q.fechaPago().atStartOfDay().atOffset(ZoneOffset.ofHours(-5)).format(FECHA_SUNAT))).toList());
    } else {
      exigir(request.cuotas().isEmpty(),"Una cuenta saldada no necesita cuotas");
      body.put("formaPago",Map.of("moneda","PEN","tipo","Contado"));
    }
    var client=new LinkedHashMap<String,Object>();
    client.put("tipoDoc",c.getReceptorTipoDoc()); client.put("numDoc",c.getReceptorNumeroDoc());
    client.put("rznSocial",c.getReceptorNombre());
    if (request.direccionReceptor()!=null && !request.direccionReceptor().isBlank())
      client.put("address",Map.of("direccion",request.direccionReceptor().trim(),"codigoPais","PE"));
    body.put("client",client);
    // La API usa el nombre 'ubigueo' en su contrato público.
    body.put("company",Map.of("ruc",config.getRuc(),"razonSocial",config.getRazonSocial(),"address",
        Map.of("ubigueo",config.getUbigeo(),"codigoPais","PE","departamento",config.getDepartamento(),
            "provincia",config.getProvincia(),"distrito",config.getDistrito(),"direccion",config.getDireccion(),"codLocal","0000")));
    body.put("mtoOperGravadas",totales.getOrDefault("10",BigDecimal.ZERO));
    body.put("mtoOperExoneradas",totales.getOrDefault("20",BigDecimal.ZERO));
    body.put("mtoOperInafectas",totales.getOrDefault("30",BigDecimal.ZERO));
    body.put("valorVenta",neto); body.put("mtoIGV",igv); body.put("totalImpuestos",igv);
    body.put("subTotal",total); body.put("mtoImpVenta",total); body.put("details",details);
    body.put("legends",List.of(Map.of("code","1000","value",MontoEnLetras.soles(total))));
    Long reserva=c.getReserva()!=null?c.getReserva().getIdReserva()
        :c.getVenta()!=null && c.getVenta().getDetalleReserva()!=null
            ?c.getVenta().getDetalleReserva().getReserva().getIdReserva():null;
    if (reserva!=null) body.put("observacion","Reserva R"+reserva);
    return new Documento(json.writeValueAsString(body),neto,igv,total);
  }
}
