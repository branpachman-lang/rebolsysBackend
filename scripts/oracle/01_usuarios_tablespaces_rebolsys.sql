-- REBOLSYS | Oracle | Ejecutar como SYSTEM en la PDB del proyecto.
-- Usar SQL*Plus o SQLcl para ejecutar el archivo con @. El servicio por defecto
-- en el backend es FREEPDB1. Comprobar el servicio de la conexion actual.
--
-- Referencias de Unidad 2:
-- CODIGO/1.CREACION_USUARIO/2.CREACION_USUARIO_COMPLETA.txt
-- CODIGO/2.PRIVILEGIOS/1.PRIVILEGIO_DE_SISTEMA.txt y 0.COMANDOS_INICIALES.txt
-- Sesion 3/CODIGO/CODIGO_TABLESPACE: ejemplos 1, 3, 12 y 13.
--
-- Los dos tablespaces son compartidos por los cinco usuarios.
-- DDL confirma cambios implicitamente. Ejecutar una vez en la PDB elegida.

SET ECHO OFF
SET VERIFY OFF
SET DEFINE ON
SET LINESIZE 180
SET PAGESIZE 50
WHENEVER OSERROR EXIT FAILURE
WHENEVER SQLERROR EXIT SQL.SQLCODE

SELECT SYS_CONTEXT('USERENV', 'CON_NAME') AS contenedor FROM dual;

DECLARE
    v_existentes PLS_INTEGER;
BEGIN
    IF SYS_CONTEXT('USERENV', 'CON_NAME') IN ('CDB$ROOT', 'PDB$SEED') THEN
        RAISE_APPLICATION_ERROR(-20001, 'Conectate a la PDB del proyecto como SYSTEM.');
    END IF;

    SELECT COUNT(*) INTO v_existentes
    FROM dba_tablespaces
    WHERE tablespace_name IN ('TBS_REBOLSYS', 'TMP_REBOLSYS');

    IF v_existentes > 0 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Ya existe un tablespace de este script. Revisa el estado antes de continuar.');
    END IF;

    SELECT COUNT(*) INTO v_existentes
    FROM dba_users
    WHERE username IN ('SEBASTIANGAMARRA_ADMIN', 'BRANDON_PACHO', 'YEREMIN_CCAPA',
                       'ESTHER_LANDA', 'JAMES_TEQUEN');

    IF v_existentes > 0 THEN
        RAISE_APPLICATION_ERROR(-20003, 'Ya existe un usuario de este script. Revisa el estado antes de continuar.');
    END IF;
END;
/

PROMPT === Archivos actuales de la PDB para ubicar el directorio de Oracle ===
COLUMN file_name FORMAT A110
SELECT tablespace_name, file_name FROM dba_data_files ORDER BY file_id;
SELECT tablespace_name, file_name FROM dba_temp_files ORDER BY file_id;

-- Indicar rutas completas de archivos nuevos en el servidor de Oracle.
-- Ejemplos de nombre: tbs_rebolsys_01.dbf y tmp_rebolsys_01.dbf.
-- Escribir rutas sin comillas. Elegir contrasenas distintas sin comillas dobles.
ACCEPT rb_datafile CHAR PROMPT 'Ruta completa para el DATAFILE nuevo: '
ACCEPT rb_tempfile CHAR PROMPT 'Ruta completa para el TEMPFILE nuevo: '
ACCEPT rb_clave_dba CHAR PROMPT 'Contrasena para SEBASTIANGAMARRA_ADMIN: ' HIDE
ACCEPT rb_clave_dev01 CHAR PROMPT 'Contrasena para BRANDON_PACHO: ' HIDE
ACCEPT rb_clave_dev02 CHAR PROMPT 'Contrasena para YEREMIN_CCAPA: ' HIDE
ACCEPT rb_clave_dev03 CHAR PROMPT 'Contrasena para ESTHER_LANDA: ' HIDE
ACCEPT rb_clave_dev04 CHAR PROMPT 'Contrasena para JAMES_TEQUEN: ' HIDE

PROMPT === Creacion de tablespaces ===
CREATE TABLESPACE TBS_REBOLSYS
    DATAFILE '&rb_datafile' SIZE 500M AUTOEXTEND OFF;

CREATE TEMPORARY TABLESPACE TMP_REBOLSYS
    TEMPFILE '&rb_tempfile' SIZE 250M AUTOEXTEND OFF;

PROMPT === Creacion de los cinco usuarios locales ===
CREATE USER SEBASTIANGAMARRA_ADMIN IDENTIFIED BY "&rb_clave_dba"
    DEFAULT TABLESPACE TBS_REBOLSYS
    TEMPORARY TABLESPACE TMP_REBOLSYS
    QUOTA UNLIMITED ON TBS_REBOLSYS
    PROFILE DEFAULT
    ACCOUNT UNLOCK;
UNDEFINE rb_clave_dba

CREATE USER BRANDON_PACHO IDENTIFIED BY "&rb_clave_dev01"
    DEFAULT TABLESPACE TBS_REBOLSYS
    TEMPORARY TABLESPACE TMP_REBOLSYS
    QUOTA 0 ON TBS_REBOLSYS
    PROFILE DEFAULT
    ACCOUNT UNLOCK;
UNDEFINE rb_clave_dev01

CREATE USER YEREMIN_CCAPA IDENTIFIED BY "&rb_clave_dev02"
    DEFAULT TABLESPACE TBS_REBOLSYS
    TEMPORARY TABLESPACE TMP_REBOLSYS
    QUOTA 0 ON TBS_REBOLSYS
    PROFILE DEFAULT
    ACCOUNT UNLOCK;
UNDEFINE rb_clave_dev02

CREATE USER ESTHER_LANDA IDENTIFIED BY "&rb_clave_dev03"
    DEFAULT TABLESPACE TBS_REBOLSYS
    TEMPORARY TABLESPACE TMP_REBOLSYS
    QUOTA 0 ON TBS_REBOLSYS
    PROFILE DEFAULT
    ACCOUNT UNLOCK;
UNDEFINE rb_clave_dev03

CREATE USER JAMES_TEQUEN IDENTIFIED BY "&rb_clave_dev04"
    DEFAULT TABLESPACE TBS_REBOLSYS
    TEMPORARY TABLESPACE TMP_REBOLSYS
    QUOTA 0 ON TBS_REBOLSYS
    PROFILE DEFAULT
    ACCOUNT UNLOCK;
UNDEFINE rb_clave_dev04

PROMPT === Asignacion de privilegios ===
GRANT CREATE SESSION TO SEBASTIANGAMARRA_ADMIN;
GRANT DBA TO SEBASTIANGAMARRA_ADMIN;

-- CREATE PROCEDURE habilita procedimientos, funciones y paquetes.
-- CREATE TRIGGER permite triggers sobre objetos del esquema propio.
-- Las vistas y rutinas que usen objetos ajenos requieren permisos directos
-- sobre los objetos correspondientes cuando estos se definan.
GRANT CREATE SESSION, CREATE PROCEDURE, CREATE TRIGGER, CREATE VIEW
    TO BRANDON_PACHO, YEREMIN_CCAPA, ESTHER_LANDA, JAMES_TEQUEN;

UNDEFINE rb_datafile rb_tempfile

PROMPT === Comprobacion de los tablespaces y tamanos ===
SELECT t.tablespace_name, t.contents, f.bytes / 1024 / 1024 AS mb, f.autoextensible
FROM dba_tablespaces t
JOIN dba_data_files f ON f.tablespace_name = t.tablespace_name
WHERE t.tablespace_name = 'TBS_REBOLSYS'
UNION ALL
SELECT t.tablespace_name, t.contents, f.bytes / 1024 / 1024 AS mb, f.autoextensible
FROM dba_tablespaces t
JOIN dba_temp_files f ON f.tablespace_name = t.tablespace_name
WHERE t.tablespace_name = 'TMP_REBOLSYS'
ORDER BY 1;

PROMPT === Usuarios y tablespaces asignados ===
SELECT username, account_status, default_tablespace, temporary_tablespace
FROM dba_users
WHERE username IN ('SEBASTIANGAMARRA_ADMIN', 'BRANDON_PACHO', 'YEREMIN_CCAPA',
                   'ESTHER_LANDA', 'JAMES_TEQUEN')
ORDER BY username;

PROMPT === Roles y privilegios directos ===
SELECT grantee, granted_role FROM dba_role_privs
WHERE grantee IN ('SEBASTIANGAMARRA_ADMIN', 'BRANDON_PACHO', 'YEREMIN_CCAPA',
                  'ESTHER_LANDA', 'JAMES_TEQUEN')
ORDER BY grantee, granted_role;

SELECT grantee, privilege FROM dba_sys_privs
WHERE grantee IN ('SEBASTIANGAMARRA_ADMIN', 'BRANDON_PACHO', 'YEREMIN_CCAPA',
                  'ESTHER_LANDA', 'JAMES_TEQUEN')
ORDER BY grantee, privilege;
