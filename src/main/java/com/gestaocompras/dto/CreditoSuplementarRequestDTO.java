package com.gestaocompras.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreditoSuplementarRequestDTO(
        @NotNull(message = "obrigatório") @Positive(message = "deve ser positivo") Long dotacaoOrigemId,
        @NotNull(message = "obrigatório") @Positive(message = "deve ser positivo") Long dotacaoDestinoId,
        @NotNull(message = "obrigatório") @Positive(message = "deve ser positivo")
        @Digits(integer = 15, fraction = 2) BigDecimal valor,
        String descricao,
        LocalDate data
) {
}
