package com.royaltyhouse.exception;

public class HorarioNoDisponibleException extends ReglaNegocioException {

    public HorarioNoDisponibleException() {
        super("El horario fue ocupado o no está disponible.");
    }
}
