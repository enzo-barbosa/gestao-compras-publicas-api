package com.gestaocompras.dto;

import com.gestaocompras.model.Genero;
import jakarta.validation.constraints.Size;

public record AtualizarContaRequestDTO(
        @Size(max = 100) String nome,
        Genero genero
) {
}