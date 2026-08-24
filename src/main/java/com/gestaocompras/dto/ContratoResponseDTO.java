package com.gestaocompras.dto;

import com.gestaocompras.model.Contrato;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratoResponseDTO(
        Long id,
        String numero,
        String objeto,
        BigDecimal valorTotal,
        Integer duracaoMeses,
        LocalDate dataInicio,
        LocalDate dataFimPrevista,
        String status,
        BigDecimal saldoRestante,
        BigDecimal valorMensal,
        Long dotacaoId,
        String dotacaoCodigo,
        Long fornecedorId,
        String fornecedorNome,
        Long licitacaoId,
        String licitacaoNumeroEdital
) {

    public static ContratoResponseDTO from(Contrato contrato) {
        return new ContratoResponseDTO(
                contrato.getId(),
                contrato.getNumero(),
                contrato.getObjeto(),
                contrato.getValorTotal(),
                contrato.getDuracaoMeses(),
                contrato.getDataInicio(),
                contrato.calcularDataFimPrevista(),
                contrato.getStatus().name(),
                contrato.getSaldoRestante(),
                contrato.calcularValorMensal(),
                contrato.getDotacao().getId(),
                contrato.getDotacao().getCodigo(),
                contrato.getFornecedor().getId(),
                contrato.getFornecedor().getNome(),
                contrato.getLicitacao() == null ? null : contrato.getLicitacao().getId(),
                contrato.getLicitacao() == null ? null : contrato.getLicitacao().getNumeroEdital());
    }
}
