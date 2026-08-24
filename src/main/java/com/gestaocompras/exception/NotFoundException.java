package com.gestaocompras.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String mensagem) {
        super(mensagem);
    }

    public NotFoundException(String recurso, Object id) {
        super("%s com id %s não encontrado.".formatted(recurso, id));
    }
}
