package pe.com.rebolsysBackend.service.impl;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import pe.com.rebolsysBackend.dto.ApisPeruDniResponseDTO;
import pe.com.rebolsysBackend.dto.ApisPeruRucResponseDTO;
import pe.com.rebolsysBackend.dto.DocumentoConsultaResponseDTO;
import pe.com.rebolsysBackend.exception.RecursoNoEncontradoException;
import pe.com.rebolsysBackend.exception.ServicioExternoException;
import pe.com.rebolsysBackend.service.service.DocumentoConsultaService;

@Service
public class DocumentoConsultaServiceImpl implements DocumentoConsultaService {

    private final RestClient apisPeruRestClient;
    private final String token;

    public DocumentoConsultaServiceImpl(RestClient apisPeruRestClient, @Value("${apis-peru.token:}") String token) {
        this.apisPeruRestClient = apisPeruRestClient;
        this.token = token;
    }

    @Override
    public DocumentoConsultaResponseDTO consultarDni(String numero) {
        ApisPeruDniResponseDTO persona = consultar("dni", numero, ApisPeruDniResponseDTO.class);
        if (persona == null || persona.getDni() == null) throw new RecursoNoEncontradoException(
            "No se encontró el DNI en APIs Perú. Puedes completar los datos manualmente."
        );
        if (!numero.equals(persona.getDni()) || persona.getNombres() == null || persona.getNombres().isBlank()) {
            throw respuestaInvalida();
        }
        String apellidos = Stream.of(persona.getApellidoPaterno(), persona.getApellidoMaterno())
            .filter(valor -> valor != null && !valor.isBlank())
            .map(String::trim)
            .collect(Collectors.joining(" "));
        if (apellidos.isBlank()) throw respuestaInvalida();
        return new DocumentoConsultaResponseDTO(numero, persona.getNombres().trim(), apellidos, null, null);
    }

    @Override
    public DocumentoConsultaResponseDTO consultarRuc(String numero) {
        ApisPeruRucResponseDTO empresa = consultar("ruc", numero, ApisPeruRucResponseDTO.class);
        if (empresa == null || empresa.getRuc() == null) throw new RecursoNoEncontradoException(
            "No se encontró el RUC en APIs Perú. Puedes completar los datos manualmente."
        );
        if (
            !numero.equals(empresa.getRuc()) || empresa.getRazonSocial() == null || empresa.getRazonSocial().isBlank()
        ) {
            throw respuestaInvalida();
        }
        return new DocumentoConsultaResponseDTO(
            numero,
            null,
            null,
            empresa.getRazonSocial().trim(),
            empresa.getDireccion()
        );
    }

    private <T> T consultar(String tipo, String numero, Class<T> respuesta) {
        if (token == null || token.isBlank()) throw new ServicioExternoException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "La consulta de DNI/RUC no está configurada. Configura APISPERU_TOKEN en el backend o completa los datos manualmente."
        );
        try {
            // El contrato oficial exige el token como query parameter. Nunca se devuelve al navegador.
            return apisPeruRestClient
                .get()
                .uri(builder ->
                    builder
                        .path("/{tipo}/{numero}")
                        .queryParam("token", "{token}")
                        .build(Map.of("tipo", tipo, "numero", numero, "token", token))
                )
                .retrieve()
                .body(respuesta);
        } catch (RestClientResponseException ex) {
            int codigo = ex.getStatusCode().value();
            if (codigo == 404) throw new RecursoNoEncontradoException(
                "El documento no fue encontrado en APIs Perú. Puedes completar los datos manualmente."
            );
            if (codigo == 401 || codigo == 403) throw new ServicioExternoException(
                HttpStatus.BAD_GATEWAY,
                "APIs Perú rechazó la credencial del backend. Revisa APISPERU_TOKEN."
            );
            if (codigo == 429) throw new ServicioExternoException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Se alcanzó el límite de consultas de APIs Perú. Intenta después o completa los datos manualmente."
            );
            throw new ServicioExternoException(
                HttpStatus.BAD_GATEWAY,
                "APIs Perú no pudo completar la consulta. Intenta después o completa los datos manualmente."
            );
        } catch (ResourceAccessException ex) {
            throw new ServicioExternoException(
                HttpStatus.GATEWAY_TIMEOUT,
                "No se pudo obtener respuesta de APIs Perú dentro del tiempo permitido. Puedes completar los datos manualmente."
            );
        } catch (RestClientException ex) {
            throw respuestaInvalida();
        }
    }

    private ServicioExternoException respuestaInvalida() {
        return new ServicioExternoException(
            HttpStatus.BAD_GATEWAY,
            "La respuesta de APIs Perú no contiene los datos esperados. Puedes completar los datos manualmente."
        );
    }
}
