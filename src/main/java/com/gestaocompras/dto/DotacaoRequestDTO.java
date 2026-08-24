package com.gestaocompras.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record DotacaoRequestDTO(
        @NotBlank(message = "obrigatório") String codigo,
        @NotBlank(message = "obrigatória") String descricao,
        @NotNull(message = "obrigatório") @Positive(message = "deve ser positivo")
        @Digits(integer = 15, fraction = 2) BigDecimal saldoInicial,
        @NotNull(message = "obrigatório") @Min(value = 2000, message = "deve ser um ano válido") Integer anoExercicio
) {
}
