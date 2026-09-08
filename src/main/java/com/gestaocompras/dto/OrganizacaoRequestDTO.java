package com.gestaocompras.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizacaoRequestDTO(
        @NotBlank(message = "obrigatório") @Size(min = 3, max = 120,
                message = "deve ter entre 3 e 120 caracteres") String nome
) {
}