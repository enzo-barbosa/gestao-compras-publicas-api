package com.gestaocompras.dto;

import com.gestaocompras.model.PapelOrganizacao;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConviteRequestDTO(
        @Email(message = "inválido") @Size(max = 150) String email,
        @Size(max = 24) String codigo,
        @NotNull(message = "obrigatório") PapelOrganizacao papel
) {
}