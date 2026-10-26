CREATE TABLE reserva_grupo (
    idGrupo NUMBER GENERATED ALWAYS AS IDENTITY,
    claveRegistro VARCHAR2(64) NOT NULL,
    firmaSolicitud VARCHAR2(64) NOT NULL,
    fechaRegistro TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT reserva_grupo_pk PRIMARY KEY (idGrupo),
    CONSTRAINT reserva_grupo_clave_uq UNIQUE (claveRegistro)
);
ALTER TABLE reserva ADD idGrupo NUMBER;
ALTER TABLE reserva ADD CONSTRAINT reserva_grupo_fk FOREIGN KEY (idGrupo) REFERENCES reserva_grupo(idGrupo);
CREATE INDEX reserva_grupo_ix ON reserva(idGrupo);
CREATE INDEX reserva_intervalo_ix ON reserva(idHabitacion, estadoReserva, fechaIngreso, fechaSalida);
