package com.gestaocompras.dto;

import com.gestaocompras.model.CreditoSuplementar;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreditoSuplementarResponseDTO(
        Long id,
        Long dotacaoOrigemId,
        String dotacaoOrigemCodigo,
        Long dotacaoDestinoId,
        String dotacaoDestinoCodigo,
        BigDecimal valor,
        String descricao,
        LocalDate data
) {

    public static CreditoSuplementarResponseDTO from(CreditoSuplementar credito) {
        return new CreditoSuplementarResponseDTO(
                credito.getId(),
                credito.getDotacaoOrigem().getId(),
                credito.getDotacaoOrigem().getCodigo(),
                credito.getDotacaoDestino().getId(),
                credito.getDotacaoDestino().getCodigo(),
                credito.getValor(),
                credito.getDescricao(),
                credito.getData());
    }
}
