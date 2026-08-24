package com.gestaocompras.dto;

import com.gestaocompras.model.Licitacao;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LicitacaoResponseDTO(
        Long id,
        String numeroEdital,
        String modalidade,
        String objeto,
        LocalDate dataAbertura,
        LocalDate dataEncerramento,
        String status,
        BigDecimal valorEstimado,
        VencedorResumo vencedor
) {

    public record VencedorResumo(Long id, String nome, String cnpj) {
    }

    public static LicitacaoResponseDTO from(Licitacao licitacao) {
        var vencedor = licitacao.getVencedor();
        return new LicitacaoResponseDTO(
                licitacao.getId(),
                licitacao.getNumeroEdital(),
                licitacao.getModalidade().name(),
                licitacao.getObjeto(),
                licitacao.getDataAbertura(),
                licitacao.getDataEncerramento(),
                licitacao.getStatus().name(),
                licitacao.getValorEstimado(),
                vencedor == null ? null
                        : new VencedorResumo(vencedor.getId(), vencedor.getNome(), vencedor.getCnpj()));
    }
}
