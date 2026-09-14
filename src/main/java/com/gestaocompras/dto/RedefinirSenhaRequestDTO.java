package com.gestaocompras.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequestDTO(
        @NotBlank(message = "obrigatório") @Email(message = "inválido") @Size(max = 150)
        String email,
        @NotBlank(message = "obrigatório") @Size(min = 6, max = 6,
                message = "deve ter 6 dígitos") String codigo,
        @NotBlank(message = "obrigatória") @Size(min = 8, max = 100,
                message = "deve ter entre 8 e 100 caracteres") String novaSenha
) {
}