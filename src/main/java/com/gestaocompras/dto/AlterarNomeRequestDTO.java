package com.gestaocompras.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarNomeRequestDTO(
        @NotBlank(message = "obrigatório") @Size(max = 100) String nome
) {
}