package com.gestaocompras.dto;

import com.gestaocompras.model.Empenho;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EmpenhoResponseDTO(
        Long id,
        Long contratoId,
        String contratoNumero,
        Long dotacaoId,
        String dotacaoCodigo,
        String fornecedorNome,
        Integer mesReferencia,
        Integer anoReferencia,
        BigDecimal valor,
        String status,
        LocalDate dataEmissao,
        Long usuarioId
) {

    public static EmpenhoResponseDTO from(Empenho empenho) {
        var contrato = empenho.getContrato();
        var dotacao = contrato.getDotacao();
        return new EmpenhoResponseDTO(
                empenho.getId(),
                contrato.getId(),
                contrato.getNumero(),
                dotacao.getId(),
                dotacao.getCodigo(),
                contrato.getFornecedor().getNome(),
                empenho.getMesReferencia(),
                empenho.getAnoReferencia(),
                empenho.getValor(),
                empenho.getStatus().name(),
                empenho.getDataEmissao(),
                empenho.getUsuario() == null ? null : empenho.getUsuario().getId());
    }
}
