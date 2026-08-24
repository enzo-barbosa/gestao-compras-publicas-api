package com.gestaocompras.dto;

import com.gestaocompras.model.DotacaoOrcamentaria;
import java.math.BigDecimal;

public record DotacaoResponseDTO(
        Long id,
        String codigo,
        String descricao,
        BigDecimal saldoInicial,
        BigDecimal saldoAtual,
        Integer anoExercicio
) {

    public static DotacaoResponseDTO from(DotacaoOrcamentaria dotacao) {
        return new DotacaoResponseDTO(dotacao.getId(), dotacao.getCodigo(), dotacao.getDescricao(),
                dotacao.getSaldoInicial(), dotacao.getSaldoAtual(), dotacao.getAnoExercicio());
    }
}
