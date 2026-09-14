package com.gestaocompras.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EsqueciSenhaRequestDTO(
        @NotBlank(message = "obrigatório") @Email(message = "inválido") @Size(max = 150)
        String email
) {
}