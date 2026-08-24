package com.gestaocompras.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ContratoRequestDTO(
        @NotBlank(message = "obrigatório") @Size(max = 30) String numero,
        @NotBlank(message = "obrigatório") @Size(max = 300) String objeto,
        @NotNull(message = "obrigatório") @Positive(message = "deve ser positivo")
        @Digits(integer = 15, fraction = 2) BigDecimal valorTotal,
        @NotNull(message = "obrigatória") @Min(value = 1, message = "deve ser de pelo menos 1 mês")
        Integer duracaoMeses,
        @NotNull(message = "obrigatória") LocalDate dataInicio,
        @NotNull(message = "obrigatória") Long dotacaoId,
        Long licitacaoId,
        @NotNull(message = "obrigatório") Long fornecedorId
) {
}
