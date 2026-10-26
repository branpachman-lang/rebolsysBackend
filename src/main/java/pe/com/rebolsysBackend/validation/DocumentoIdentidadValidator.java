package pe.com.rebolsysBackend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import pe.com.rebolsysBackend.dto.ClienteRegistroRequestDTO;
import pe.com.rebolsysBackend.entity.TipoDocumento;
import pe.com.rebolsysBackend.repository.TipoDocumentoRepository;

@RequiredArgsConstructor
public class DocumentoIdentidadValidator
    implements ConstraintValidator<DocumentoIdentidadValido, ClienteRegistroRequestDTO>
{

    private final TipoDocumentoRepository tipoDocumentoRepository;

    @Override
    public boolean isValid(ClienteRegistroRequestDTO datos, ConstraintValidatorContext context) {
        if (datos == null || datos.getIdTipoDocumento() == null || datos.getNumeroDocumento() == null) return true;
        TipoDocumento tipo = tipoDocumentoRepository.findById(datos.getIdTipoDocumento()).orElse(null);
        if (tipo == null) return error(context, "idTipoDocumento", "El tipo de documento no existe");
        String nombre = tipo.getNombreDocumento().trim().toUpperCase(java.util.Locale.ROOT);
        boolean dni = "1".equals(tipo.getCodigoSunat()) || "DNI".equals(nombre);
        boolean ruc = !dni && ("6".equals(tipo.getCodigoSunat()) || "RUC".equals(nombre));
        if (dni && !datos.getNumeroDocumento().matches("[0-9]{8}")) {
            return error(context, "numeroDocumento", "El DNI debe contener exactamente 8 dígitos");
        }
        if (ruc && !datos.getNumeroDocumento().matches("[0-9]{11}")) {
            return error(context, "numeroDocumento", "El RUC debe contener exactamente 11 dígitos");
        }
        if (dni && Boolean.TRUE.equals(datos.getEmpresa())) {
            return error(context, "empresa", "El DNI corresponde a una persona natural");
        }
        return true;
    }

    private boolean error(ConstraintValidatorContext context, String campo, String mensaje) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(mensaje).addPropertyNode(campo).addConstraintViolation();
        return false;
    }
}
