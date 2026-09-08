package com.gestaocompras.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AceitarCodigoRequestDTO(
        @NotBlank(message = "obrigatório") @Size(max = 24) String codigo
) {
}