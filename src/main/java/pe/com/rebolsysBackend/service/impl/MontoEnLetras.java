package pe.com.rebolsysBackend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class MontoEnLetras {
  private MontoEnLetras() {}
  private static final String[] UNIDADES={"CERO","UNO","DOS","TRES","CUATRO","CINCO","SEIS","SIETE","OCHO","NUEVE","DIEZ","ONCE","DOCE","TRECE","CATORCE","QUINCE","DIECISÉIS","DIECISIETE","DIECIOCHO","DIECINUEVE","VEINTE","VEINTIUNO","VEINTIDÓS","VEINTITRÉS","VEINTICUATRO","VEINTICINCO","VEINTISÉIS","VEINTISIETE","VEINTIOCHO","VEINTINUEVE"};
  private static final String[] DECENAS={"","","","TREINTA","CUARENTA","CINCUENTA","SESENTA","SETENTA","OCHENTA","NOVENTA"};
  private static final String[] CENTENAS={"","CIENTO","DOSCIENTOS","TRESCIENTOS","CUATROCIENTOS","QUINIENTOS","SEISCIENTOS","SETECIENTOS","OCHOCIENTOS","NOVECIENTOS"};
  static String soles(BigDecimal monto) {
    var valor=monto.setScale(2,RoundingMode.HALF_UP);
    long entero=valor.toBigInteger().longValueExact();
    // No usar double: los céntimos y la leyenda deben corresponder exactamente.
    int centimos=valor.remainder(BigDecimal.ONE).movePointRight(2).intValueExact();
    return "SON "+numero(entero)+" CON "+String.format(java.util.Locale.ROOT,"%02d",centimos)+"/100 SOLES";
  }
  private static String numero(long n) {
    if (n<30) return UNIDADES[(int)n];
    if (n<100) return DECENAS[(int)n/10]+(n%10==0?"":" Y "+numero(n%10));
    if (n==100) return "CIEN";
    if (n<1000) return CENTENAS[(int)n/100]+(n%100==0?"":" "+numero(n%100));
    if (n<1_000_000) return (n/1000==1?"MIL":apocopar(numero(n/1000))+" MIL")+(n%1000==0?"":" "+numero(n%1000));
    return (n/1_000_000==1?"UN MILLÓN":apocopar(numero(n/1_000_000))+" MILLONES")+(n%1_000_000==0?"":" "+numero(n%1_000_000));
  }
  private static String apocopar(String n) { return n.endsWith("UNO") ? n.substring(0,n.length()-1) : n; }
}
