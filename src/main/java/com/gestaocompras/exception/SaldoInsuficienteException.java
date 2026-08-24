package com.gestaocompras.exception;

import java.math.BigDecimal;

public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(String mensagem) {
        super(mensagem);
    }

    public SaldoInsuficienteException(String recurso, BigDecimal disponivel, BigDecimal solicitado) {
        super("%s possui saldo insuficiente: disponível R$ %s, necessário R$ %s."
                .formatted(recurso, disponivel, solicitado));
    }
}
