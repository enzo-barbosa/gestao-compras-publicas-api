package com.gestaocompras.dto;

import com.gestaocompras.model.PapelOrganizacao;
import jakarta.validation.constraints.NotNull;

public record MembroPapelRequestDTO(
        @NotNull(message = "obrigatório") PapelOrganizacao papel
) {
}