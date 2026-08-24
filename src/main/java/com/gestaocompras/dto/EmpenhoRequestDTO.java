package com.gestaocompras.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EmpenhoRequestDTO(
        @NotNull(message = "obrigatório") Long contratoId,
        @NotNull(message = "obrigatório") @Min(value = 1, message = "deve estar entre 1 e 12")
        @Max(value = 12, message = "deve estar entre 1 e 12") Integer mesReferencia,
        @NotNull(message = "obrigatório") @Min(value = 1970, message = "inválido")
        Integer anoReferencia
) {
}
