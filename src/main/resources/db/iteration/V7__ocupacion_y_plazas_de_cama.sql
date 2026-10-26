-- Configuración por habitación solicitada: no ampliar ocupaciones automáticamente.
-- Los límites existentes se conservan y las excepciones comienzan en cero.
-- Ejecutar una sola vez después de V6. Cada bloque termina en /.
DECLARE
    v_existe NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_existe FROM user_tab_columns
        WHERE table_name='RESERVA_HUESPED' AND column_name='OCUPAPLAZACAMA';
    IF v_existe<>0 THEN RAISE_APPLICATION_ERROR(-20040,'V7 ya fue aplicada'); END IF;
END;
/
ALTER TABLE habitacion ADD (
    capacidadPersonas NUMBER(2),
    plazasCama NUMBER(2),
    maxMenoresSinCama NUMBER(2) DEFAULT 0 NOT NULL
)
/
UPDATE habitacion h SET (capacidadPersonas,plazasCama)=(
    SELECT t.capacidadMax,t.capacidadMax FROM tipo_habitacion t WHERE t.idTipoHabitacion=h.idTipo
)
/
ALTER TABLE habitacion MODIFY (capacidadPersonas NOT NULL,plazasCama NOT NULL)
/
ALTER TABLE habitacion ADD CONSTRAINT habitacion_personas_chk CHECK (capacidadPersonas BETWEEN 1 AND 99)
/
ALTER TABLE habitacion ADD CONSTRAINT habitacion_plazas_chk CHECK (plazasCama BETWEEN 1 AND capacidadPersonas)
/
ALTER TABLE habitacion ADD CONSTRAINT habitacion_menores_cama_chk CHECK (maxMenoresSinCama BETWEEN 0 AND capacidadPersonas-1)
/
ALTER TABLE reserva_huesped ADD ocupaPlazaCama NUMBER(1) DEFAULT 1 NOT NULL
/
ALTER TABLE reserva_huesped ADD CONSTRAINT rh_ocupa_cama_chk CHECK (ocupaPlazaCama IN (0,1))
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
        v_personas NUMBER; v_plazas NUMBER; v_max_menores NUMBER;
        v_total NUMBER; v_con_cama NUMBER; v_sin_cama NUMBER; v_estado VARCHAR2(30);
    BEGIN
        BEGIN
            SELECT h.capacidadPersonas,h.plazasCama,h.maxMenoresSinCama,d.estadoDetalle
                INTO v_personas,v_plazas,v_max_menores,v_estado
                FROM detalle_reserva d JOIN habitacion h ON h.idHabitacion=d.idHabitacion
                WHERE d.idDetalleReserva=p_reserva;
        EXCEPTION WHEN NO_DATA_FOUND THEN RETURN; END;
        SELECT COUNT(*),NVL(SUM(CASE WHEN ocupaPlazaCama=1 THEN 1 ELSE 0 END),0),
            NVL(SUM(CASE WHEN ocupaPlazaCama=0 THEN 1 ELSE 0 END),0)
            INTO v_total,v_con_cama,v_sin_cama FROM reserva_huesped WHERE idDetalleReserva=p_reserva;
        IF v_estado IN ('CONFIRMADA','EN_CURSO') THEN
            IF v_total>v_personas THEN RAISE_APPLICATION_ERROR(-20015,'Se excedió la capacidad máxima de personas de la habitación'); END IF;
            IF v_con_cama>v_plazas THEN RAISE_APPLICATION_ERROR(-20016,'Se excedieron las plazas de cama de la habitación'); END IF;
            IF v_sin_cama>v_max_menores THEN RAISE_APPLICATION_ERROR(-20017,'Se excedió el máximo de menores sin plaza de cama propia de la habitación'); END IF;
        END IF;
        FOR persona IN (
            SELECT rh.idHuespedResponsable,rh.parentescoResponsable,rh.ocupaPlazaCama,n.fechaNacimiento,r.fechaIngreso,
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
            IF persona.ocupaPlazaCama=0 AND MONTHS_BETWEEN(TRUNC(persona.fechaIngreso),TRUNC(persona.fechaNacimiento))>=84 THEN
                RAISE_APPLICATION_ERROR(-20014,'Solo los menores de 7 años al ingresar pueden alojarse sin plaza de cama propia');
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
CREATE OR REPLACE TRIGGER habitacion_ocupacion_valida
FOR UPDATE OF capacidadPersonas,plazasCama,maxMenoresSinCama ON habitacion
COMPOUND TRIGGER
    TYPE habitaciones_tab IS TABLE OF NUMBER INDEX BY VARCHAR2(50);
    habitaciones habitaciones_tab;
    AFTER EACH ROW IS
    BEGIN
        habitaciones(TO_CHAR(:NEW.idHabitacion)):=:NEW.idHabitacion;
    END AFTER EACH ROW;
    AFTER STATEMENT IS
        clave VARCHAR2(50);
    BEGIN
        clave:=habitaciones.FIRST;
        WHILE clave IS NOT NULL LOOP
            FOR estancia IN (SELECT idDetalleReserva FROM detalle_reserva
                WHERE idHabitacion=habitaciones(clave) AND estadoDetalle IN ('CONFIRMADA','EN_CURSO')) LOOP
                rb_validaciones.validar_habitacion(estancia.idDetalleReserva);
            END LOOP;
            clave:=habitaciones.NEXT(clave);
        END LOOP;
    END AFTER STATEMENT;
END;
/
CREATE OR REPLACE TRIGGER detalle_reserva_edad_ingreso
FOR INSERT OR UPDATE OF fechaIngreso,idHabitacion,estadoDetalle ON detalle_reserva
COMPOUND TRIGGER
    TYPE detalles_tab IS TABLE OF NUMBER INDEX BY VARCHAR2(50);
    detalles detalles_tab;
    AFTER EACH ROW IS
    BEGIN
        detalles(TO_CHAR(:NEW.idDetalleReserva)):=:NEW.idDetalleReserva;
    END AFTER EACH ROW;
    AFTER STATEMENT IS
        clave VARCHAR2(50);
    BEGIN
        clave:=detalles.FIRST;
        WHILE clave IS NOT NULL LOOP
            rb_validaciones.validar_habitacion(detalles(clave));
            clave:=detalles.NEXT(clave);
        END LOOP;
    END AFTER STATEMENT;
END;
/
