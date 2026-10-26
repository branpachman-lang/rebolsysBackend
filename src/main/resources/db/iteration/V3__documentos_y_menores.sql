-- Autorizado: dos campos en reserva_huesped y validaciones de documentos/edad.
-- No agrega tablas ni cambia la numeración. No elimina ni reasigna datos históricos.
ALTER TABLE reserva_huesped ADD (
    idHuespedResponsable NUMBER,
    parentescoResponsable VARCHAR2(10)
)
/
ALTER TABLE reserva_huesped ADD CONSTRAINT rh_responsable_fk
    FOREIGN KEY (idReserva,idHuespedResponsable)
    REFERENCES reserva_huesped(idReserva,idHuesped)
    DEFERRABLE INITIALLY DEFERRED
/
ALTER TABLE reserva_huesped ADD CONSTRAINT rh_parentesco_chk CHECK (
    (idHuespedResponsable IS NULL AND parentescoResponsable IS NULL)
    OR (idHuespedResponsable IS NOT NULL AND parentescoResponsable IS NOT NULL
        AND idHuespedResponsable<>idHuesped
        AND parentescoResponsable IN ('PADRE','MADRE','TUTOR'))
)
/
CREATE OR REPLACE PACKAGE rb_validaciones AS
    PROCEDURE validar_titular(p_perfil NUMBER);
    PROCEDURE validar_cliente(p_cliente NUMBER);
    PROCEDURE validar_habitacion(p_reserva NUMBER);
END rb_validaciones;
/
CREATE OR REPLACE PACKAGE BODY rb_validaciones AS
    FUNCTION hoy RETURN DATE IS
    BEGIN
        RETURN TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'America/Lima' AS DATE));
    END;
    PROCEDURE validar_titular(p_perfil NUMBER) IS
        v_fecha DATE;
    BEGIN
        BEGIN
            SELECT fechaNacimiento INTO v_fecha FROM persona_natural WHERE idPerfil=p_perfil;
        EXCEPTION WHEN NO_DATA_FOUND THEN RETURN;
        END;
        IF v_fecha IS NULL THEN
            RAISE_APPLICATION_ERROR(-20004,'La fecha de nacimiento del titular es obligatoria');
        END IF;
        IF v_fecha>hoy OR MONTHS_BETWEEN(hoy,TRUNC(v_fecha))<216 THEN
            RAISE_APPLICATION_ERROR(-20005,'El titular de la reserva debe tener al menos 18 años');
        END IF;
    END;
    PROCEDURE validar_cliente(p_cliente NUMBER) IS
        v_perfil NUMBER;
    BEGIN
        SELECT idPerfil INTO v_perfil FROM cliente WHERE idCliente=p_cliente;
        validar_titular(v_perfil);
    END;
    PROCEDURE validar_habitacion(p_reserva NUMBER) IS
    BEGIN
        FOR persona IN (
            SELECT rh.idHuespedResponsable,rh.parentescoResponsable,n.fechaNacimiento,r.fechaIngreso,
                adulto.idHuesped responsablePresente,responsable.fechaNacimiento fechaResponsable
            FROM reserva_huesped rh
            JOIN reserva r ON r.idReserva=rh.idReserva
            JOIN huesped h ON h.idHuesped=rh.idHuesped
            JOIN persona_natural n ON n.idPerfil=h.idPerfil
            LEFT JOIN reserva_huesped adulto ON adulto.idReserva=rh.idReserva
                AND adulto.idHuesped=rh.idHuespedResponsable
            LEFT JOIN huesped ha ON ha.idHuesped=adulto.idHuesped
            LEFT JOIN persona_natural responsable ON responsable.idPerfil=ha.idPerfil
            WHERE rh.idReserva=p_reserva
        ) LOOP
            IF persona.fechaNacimiento IS NULL THEN
                RAISE_APPLICATION_ERROR(-20010,'La fecha de nacimiento de cada huésped es obligatoria');
            END IF;
            IF TRUNC(persona.fechaNacimiento)>hoy THEN
                RAISE_APPLICATION_ERROR(-20003,'La fecha de nacimiento no puede ser futura');
            END IF;
            IF MONTHS_BETWEEN(TRUNC(persona.fechaIngreso),TRUNC(persona.fechaNacimiento))<216
                AND (persona.idHuespedResponsable IS NULL OR persona.parentescoResponsable IS NULL) THEN
                RAISE_APPLICATION_ERROR(-20011,'Todo menor debe tener padre, madre o tutor responsable');
            END IF;
            IF persona.idHuespedResponsable IS NOT NULL THEN
                IF persona.responsablePresente IS NULL THEN
                    RAISE_APPLICATION_ERROR(-20012,'El responsable debe alojarse en la misma habitación que el menor');
                END IF;
                IF persona.fechaResponsable IS NULL OR
                    MONTHS_BETWEEN(TRUNC(persona.fechaIngreso),TRUNC(persona.fechaResponsable))<216 THEN
                    RAISE_APPLICATION_ERROR(-20013,'El responsable del menor debe tener al menos 18 años al ingresar');
                END IF;
            END IF;
        END LOOP;
    END;
END rb_validaciones;
/
CREATE OR REPLACE TRIGGER perfil_documento_valido
BEFORE INSERT OR UPDATE OF idTipoDocumento,numeroDocumento ON perfil
FOR EACH ROW
DECLARE
    v_codigo VARCHAR2(2);
    v_nombre VARCHAR2(30);
BEGIN
    SELECT codigoSunat,UPPER(TRIM(nombreDocumento)) INTO v_codigo,v_nombre
        FROM tipo_documento WHERE idTipoDocumento=:NEW.idTipoDocumento;
    IF (v_codigo='1' OR v_nombre='DNI') AND NOT REGEXP_LIKE(:NEW.numeroDocumento,'^[0-9]{8}$') THEN
        RAISE_APPLICATION_ERROR(-20001,'El DNI debe contener exactamente 8 dígitos');
    ELSIF (v_codigo='6' OR v_nombre='RUC') AND NOT REGEXP_LIKE(:NEW.numeroDocumento,'^[0-9]{11}$') THEN
        RAISE_APPLICATION_ERROR(-20002,'El RUC debe contener exactamente 11 dígitos');
    END IF;
EXCEPTION WHEN NO_DATA_FOUND THEN
    RAISE_APPLICATION_ERROR(-20006,'El tipo de documento no existe');
END;
/
CREATE OR REPLACE TRIGGER pn_nacimiento_valido
BEFORE INSERT OR UPDATE OF fechaNacimiento ON persona_natural
FOR EACH ROW
BEGIN
    IF TRUNC(:NEW.fechaNacimiento)>TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'America/Lima' AS DATE)) THEN
        RAISE_APPLICATION_ERROR(-20003,'La fecha de nacimiento no puede ser futura');
    END IF;
END;
/
CREATE OR REPLACE TRIGGER cliente_titular_adulto
BEFORE INSERT OR UPDATE OF idPerfil ON cliente
FOR EACH ROW
BEGIN
    rb_validaciones.validar_titular(:NEW.idPerfil);
END;
/
CREATE OR REPLACE TRIGGER reserva_titular_adulto
BEFORE INSERT OR UPDATE OF idCliente ON reserva
FOR EACH ROW
BEGIN
    rb_validaciones.validar_cliente(:NEW.idCliente);
END;
/
CREATE OR REPLACE TRIGGER rh_menores_validos
FOR INSERT OR UPDATE OR DELETE ON reserva_huesped
COMPOUND TRIGGER
    TYPE reservas_tab IS TABLE OF NUMBER INDEX BY VARCHAR2(50);
    reservas reservas_tab;
    AFTER EACH ROW IS
    BEGIN
        IF INSERTING OR UPDATING THEN reservas(TO_CHAR(:NEW.idReserva)):=:NEW.idReserva; END IF;
        IF DELETING OR UPDATING THEN reservas(TO_CHAR(:OLD.idReserva)):=:OLD.idReserva; END IF;
    END AFTER EACH ROW;
    AFTER STATEMENT IS
        clave VARCHAR2(50);
    BEGIN
        clave:=reservas.FIRST;
        WHILE clave IS NOT NULL LOOP
            rb_validaciones.validar_habitacion(reservas(clave));
            clave:=reservas.NEXT(clave);
        END LOOP;
    END AFTER STATEMENT;
END;
/
CREATE OR REPLACE TRIGGER reserva_edad_ingreso
FOR UPDATE OF fechaIngreso ON reserva
COMPOUND TRIGGER
    TYPE reservas_tab IS TABLE OF NUMBER INDEX BY VARCHAR2(50);
    reservas reservas_tab;
    AFTER EACH ROW IS
    BEGIN
        reservas(TO_CHAR(:NEW.idReserva)):=:NEW.idReserva;
    END AFTER EACH ROW;
    AFTER STATEMENT IS
        clave VARCHAR2(50);
    BEGIN
        clave:=reservas.FIRST;
        WHILE clave IS NOT NULL LOOP
            rb_validaciones.validar_habitacion(reservas(clave));
            clave:=reservas.NEXT(clave);
        END LOOP;
    END AFTER STATEMENT;
END;
/
CREATE OR REPLACE TRIGGER pn_edad_relaciones
FOR INSERT OR UPDATE OF fechaNacimiento ON persona_natural
COMPOUND TRIGGER
    TYPE personas_tab IS TABLE OF NUMBER INDEX BY VARCHAR2(50);
    personas personas_tab;
    AFTER EACH ROW IS
    BEGIN
        personas(TO_CHAR(:NEW.idPerfil)):=:NEW.idPerfil;
    END AFTER EACH ROW;
    AFTER STATEMENT IS
        clave VARCHAR2(50);
    BEGIN
        clave:=personas.FIRST;
        WHILE clave IS NOT NULL LOOP
            FOR titular IN (SELECT idCliente FROM cliente WHERE idPerfil=personas(clave)) LOOP
                rb_validaciones.validar_titular(personas(clave));
            END LOOP;
            FOR estancia IN (SELECT DISTINCT rh.idReserva FROM reserva_huesped rh
                JOIN huesped h ON h.idHuesped=rh.idHuesped WHERE h.idPerfil=personas(clave)) LOOP
                rb_validaciones.validar_habitacion(estancia.idReserva);
            END LOOP;
            clave:=personas.NEXT(clave);
        END LOOP;
    END AFTER STATEMENT;
END;
/
-- Auditoría de los menores anteriores, sin asignar responsables automáticamente:
-- SELECT rh.idReserva,rh.idHuesped FROM reserva_huesped rh
-- JOIN reserva r ON r.idReserva=rh.idReserva JOIN huesped h ON h.idHuesped=rh.idHuesped
-- JOIN persona_natural n ON n.idPerfil=h.idPerfil
-- WHERE MONTHS_BETWEEN(TRUNC(r.fechaIngreso),TRUNC(n.fechaNacimiento))<216
-- AND rh.idHuespedResponsable IS NULL;
