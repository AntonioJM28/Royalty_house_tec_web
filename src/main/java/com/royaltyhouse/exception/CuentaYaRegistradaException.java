package com.royaltyhouse.exception;

public class CuentaYaRegistradaException extends RuntimeException {

    public CuentaYaRegistradaException() {
        super("Ya existe una cuenta con ese correo.");
    }
}
