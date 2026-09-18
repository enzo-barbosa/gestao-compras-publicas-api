package com.gestaocompras.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MembroSenhaRequestDTO(
        @NotBlank(message = "obrigatória") @Size(min = 8, max = 100,
                message = "deve ter entre 8 e 100 caracteres") String novaSenha
) {
}
