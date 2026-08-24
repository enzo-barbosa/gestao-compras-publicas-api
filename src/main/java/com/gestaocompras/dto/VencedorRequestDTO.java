package com.gestaocompras.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VencedorRequestDTO(
        @NotNull(message = "obrigatório") @Positive(message = "deve ser positivo") Long fornecedorId
) {
}
