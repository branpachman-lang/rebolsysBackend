package pe.com.rebolsysBackend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EstadoItem {
    ACTIVO("Activo"),
    INACTIVO("Inactivo"),
    EN_HABITACION("En habitacion");

    private final String valor;

    EstadoItem(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static EstadoItem desdeValor(String valor) {
        for (EstadoItem estado : values()) {
            if (estado.valor.equals(valor)) return estado;
        }
        throw new IllegalArgumentException("Estado de ítem no admitido: " + valor);
    }
}
