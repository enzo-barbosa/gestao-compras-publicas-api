package com.gestaocompras.exception;

public class NaoMembroException extends RuntimeException {

    public NaoMembroException() {
        super("Você não é membro desta organização.");
    }
}