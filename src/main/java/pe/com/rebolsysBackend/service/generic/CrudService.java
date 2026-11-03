package pe.com.rebolsysBackend.service.generic;

/**
 * Contrato de mantenimiento compartido con la estructura de Pharma.
 * REQ y RES son DTO; cada módulo define sus reglas y transacciones.
 * delete puede ser una baja lógica o rechazar registros con historial.
 */
public interface CrudService<REQ, RES, ID> {
    RES create(REQ request);
    RES update(ID id, REQ request);
    RES read(ID id);
    void delete(ID id);
    Iterable<RES> readAll();
}
