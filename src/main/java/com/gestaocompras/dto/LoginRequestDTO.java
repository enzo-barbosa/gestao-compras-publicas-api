package com.gestaocompras.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "obrigatório") @Email(message = "inválido") String email,
        @NotBlank(message = "obrigatória") String senha
) {
}
