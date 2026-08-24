package com.gestaocompras.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErroResposta(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        List<String> detalhes
) {

    public static ErroResposta of(int status, String erro, String mensagem) {
        return new ErroResposta(LocalDateTime.now(), status, erro, mensagem, List.of());
    }

    public static ErroResposta of(int status, String erro, String mensagem, List<String> detalhes) {
        return new ErroResposta(LocalDateTime.now(), status, erro, mensagem, detalhes);
    }
}
