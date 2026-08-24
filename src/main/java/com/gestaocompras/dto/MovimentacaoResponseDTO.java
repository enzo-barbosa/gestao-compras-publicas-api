package com.gestaocompras.dto;

import com.gestaocompras.model.MovimentacaoDotacao;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimentacaoResponseDTO(
        Long id,
        String tipo,
        BigDecimal valor,
        String descricao,
        LocalDateTime dataHora
) {

    public static MovimentacaoResponseDTO from(MovimentacaoDotacao movimentacao) {
        return new MovimentacaoResponseDTO(movimentacao.getId(), movimentacao.getTipo().name(),
                movimentacao.getValor(), movimentacao.getDescricao(), movimentacao.getDataHora());
    }
}
