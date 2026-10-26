-- Migración respaldada autorizada explícitamente el 26/10/2026.
-- Aplicar una sola vez después de V5, junto con el código de cabecera/detalle.
-- Cada bloque termina en /. Oracle confirma DDL automáticamente.
DECLARE
    v_cantidad NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_cantidad FROM user_tables WHERE table_name='DETALLE_RESERVA';
    IF v_cantidad<>0 THEN RAISE_APPLICATION_ERROR(-20030,'V6 ya fue aplicada o existe detalle_reserva'); END IF;
    SELECT COUNT(*) INTO v_cantidad FROM (
        SELECT idGrupo FROM reserva WHERE idGrupo IS NOT NULL GROUP BY idGrupo
        HAVING COUNT(DISTINCT idCliente)>1 OR COUNT(DISTINCT idUsuario)>1
    );
    IF v_cantidad<>0 THEN RAISE_APPLICATION_ERROR(-20031,'Hay grupos con titulares o usuarios distintos: revisar antes de migrar'); END IF;
    SELECT COUNT(*) INTO v_cantidad FROM reserva_grupo g
        WHERE NOT EXISTS(SELECT 1 FROM reserva r WHERE r.idGrupo=g.idGrupo);
    IF v_cantidad<>0 THEN RAISE_APPLICATION_ERROR(-20032,'Hay cabeceras sin habitaciones: revisar antes de migrar'); END IF;
END;
/
ALTER TABLE reserva_grupo ADD (idCliente NUMBER,idUsuario NUMBER,estadoReserva VARCHAR2(30))
/
UPDATE reserva_grupo g SET (idCliente,idUsuario,estadoReserva)=(
    SELECT MIN(r.idCliente),MIN(r.idUsuario),
        CASE WHEN MAX(CASE WHEN r.estadoReserva='EN_CURSO' THEN 1 ELSE 0 END)=1 THEN 'EN_CURSO'
             WHEN MAX(CASE WHEN r.estadoReserva='CONFIRMADA' THEN 1 ELSE 0 END)=1 THEN 'CONFIRMADA'
             WHEN MIN(CASE WHEN r.estadoReserva='CANCELADA' THEN 1 ELSE 0 END)=1 THEN 'CANCELADA'
             ELSE 'FINALIZADA' END
    FROM reserva r WHERE r.idGrupo=g.idGrupo
)
/
DECLARE
    v_cabecera NUMBER;
BEGIN
    FOR fila IN (SELECT idReserva,idCliente,idUsuario,estadoReserva FROM reserva WHERE idGrupo IS NULL ORDER BY idReserva) LOOP
        INSERT INTO reserva_grupo(claveRegistro,firmaSolicitud,idCliente,idUsuario,estadoReserva)
            VALUES('migracion-v6-'||TO_CHAR(fila.idReserva),
                   LOWER(RAWTOHEX(STANDARD_HASH('migracion-v6-'||TO_CHAR(fila.idReserva),'SHA256'))),
                   fila.idCliente,fila.idUsuario,fila.estadoReserva)
            RETURNING idGrupo INTO v_cabecera;
        UPDATE reserva SET idGrupo=v_cabecera WHERE idReserva=fila.idReserva;
    END LOOP;
END;
/
COMMIT
/
DROP TRIGGER reserva_titular_adulto
/
DROP TRIGGER reserva_edad_ingreso
/
ALTER TABLE reserva RENAME TO detalle_reserva
/
ALTER TABLE detalle_reserva RENAME COLUMN idReserva TO idDetalleReserva
/
ALTER TABLE detalle_reserva RENAME COLUMN idGrupo TO idReserva
/
ALTER TABLE detalle_reserva RENAME COLUMN estadoReserva TO estadoDetalle
/
ALTER TABLE reserva_grupo RENAME TO reserva
/
ALTER TABLE reserva RENAME COLUMN idGrupo TO idReserva
/
ALTER TABLE detalle_reserva DROP CONSTRAINT reserva_cliente_fk
/
ALTER TABLE detalle_reserva DROP CONSTRAINT reserva_usuario_fk
/
ALTER TABLE detalle_reserva DROP (idCliente,idUsuario)
/
ALTER TABLE detalle_reserva MODIFY (idReserva NOT NULL)
/
ALTER TABLE detalle_reserva RENAME CONSTRAINT reserva_pk TO detalle_reserva_pk
/
ALTER TABLE detalle_reserva RENAME CONSTRAINT reserva_grupo_fk TO detalle_reserva_cabecera_fk
/
ALTER TABLE detalle_reserva RENAME CONSTRAINT reserva_habitacion_fk TO detalle_reserva_habitacion_fk
/
ALTER TABLE detalle_reserva RENAME CONSTRAINT reserva_costo_chk TO detalle_reserva_costo_chk
/
ALTER TABLE detalle_reserva RENAME CONSTRAINT reserva_fechas_chk TO detalle_reserva_fechas_chk
/
ALTER TABLE detalle_reserva RENAME CONSTRAINT reserva_estado_chk TO detalle_reserva_estado_chk
/
ALTER TABLE reserva RENAME CONSTRAINT reserva_grupo_pk TO reserva_pk
/
ALTER TABLE reserva RENAME CONSTRAINT reserva_grupo_clave_uq TO reserva_clave_uq
/
ALTER TABLE reserva MODIFY (idCliente NOT NULL,idUsuario NOT NULL,estadoReserva NOT NULL)
/
ALTER TABLE reserva ADD CONSTRAINT reserva_cliente_fk FOREIGN KEY(idCliente) REFERENCES cliente(idCliente)
/
ALTER TABLE reserva ADD CONSTRAINT reserva_usuario_fk FOREIGN KEY(idUsuario) REFERENCES usuario(idUsuario)
/
ALTER TABLE reserva ADD CONSTRAINT reserva_estado_chk CHECK (estadoReserva IN ('CONFIRMADA','EN_CURSO','FINALIZADA','CANCELADA'))
/
ALTER TABLE reserva_huesped RENAME COLUMN idReserva TO idDetalleReserva
/
ALTER TABLE reserva_huesped RENAME CONSTRAINT rh_reserva_fk TO rh_detalle_reserva_fk
/
ALTER TABLE consumo RENAME COLUMN idReserva TO idDetalleReserva
/
ALTER TABLE consumo RENAME CONSTRAINT consumo_reserva_fk TO consumo_detalle_reserva_fk
/
ALTER TABLE venta RENAME COLUMN idReserva TO idDetalleReserva
/
ALTER TABLE venta RENAME CONSTRAINT venta_reserva_fk TO venta_detalle_reserva_fk
/
ALTER TABLE informe_revision RENAME COLUMN idReserva TO idDetalleReserva
/
ALTER TABLE informe_revision RENAME CONSTRAINT informe_reserva_fk TO informe_detalle_reserva_fk
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
            JOIN detalle_reserva r ON r.idDetalleReserva=rh.idDetalleReserva
            JOIN huesped h ON h.idHuesped=rh.idHuesped
            JOIN persona_natural n ON n.idPerfil=h.idPerfil
            LEFT JOIN reserva_huesped adulto ON adulto.idDetalleReserva=rh.idDetalleReserva
                AND adulto.idHuesped=rh.idHuespedResponsable
            LEFT JOIN huesped ha ON ha.idHuesped=adulto.idHuesped
            LEFT JOIN persona_natural responsable ON responsable.idPerfil=ha.idPerfil
            WHERE rh.idDetalleReserva=p_reserva
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
        IF INSERTING OR UPDATING THEN reservas(TO_CHAR(:NEW.idDetalleReserva)):=:NEW.idDetalleReserva; END IF;
        IF DELETING OR UPDATING THEN reservas(TO_CHAR(:OLD.idDetalleReserva)):=:OLD.idDetalleReserva; END IF;
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
CREATE OR REPLACE TRIGGER detalle_reserva_edad_ingreso
FOR UPDATE OF fechaIngreso ON detalle_reserva
COMPOUND TRIGGER
    TYPE reservas_tab IS TABLE OF NUMBER INDEX BY VARCHAR2(50);
    reservas reservas_tab;
    AFTER EACH ROW IS
    BEGIN
        reservas(TO_CHAR(:NEW.idDetalleReserva)):=:NEW.idDetalleReserva;
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
            FOR estancia IN (SELECT DISTINCT rh.idDetalleReserva FROM reserva_huesped rh
                JOIN huesped h ON h.idHuesped=rh.idHuesped WHERE h.idPerfil=personas(clave)) LOOP
                rb_validaciones.validar_habitacion(estancia.idDetalleReserva);
            END LOOP;
            clave:=personas.NEXT(clave);
        END LOOP;
    END AFTER STATEMENT;
END;
/

