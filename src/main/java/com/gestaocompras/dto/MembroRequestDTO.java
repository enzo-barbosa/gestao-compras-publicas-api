package com.gestaocompras.dto;

import com.gestaocompras.model.PapelOrganizacao;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MembroRequestDTO(
        @NotBlank(message = "obrigatório") @Email(message = "inválido") @Size(max = 150)
        String email,
        @NotNull(message = "obrigatório") PapelOrganizacao papel
) {
}