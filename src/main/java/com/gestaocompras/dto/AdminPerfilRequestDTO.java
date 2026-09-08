package com.gestaocompras.dto;

import com.gestaocompras.model.Perfil;
import jakarta.validation.constraints.NotNull;

public record AdminPerfilRequestDTO(
        @NotNull(message = "obrigatório") Perfil perfil
) {
}