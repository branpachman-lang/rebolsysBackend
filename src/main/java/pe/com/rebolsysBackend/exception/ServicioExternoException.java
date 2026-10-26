package pe.com.rebolsysBackend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ServicioExternoException extends RuntimeException {

    private final HttpStatus status;

    public ServicioExternoException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
