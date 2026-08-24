package com.gestaocompras.dto;

import com.gestaocompras.model.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistroRequestDTO(
        @NotBlank(message = "obrigatório") @Size(max = 100) String nome,
        @NotBlank(message = "obrigatório") @Email(message = "inválido") @Size(max = 150)
        String email,
        @NotBlank(message = "obrigatória") @Size(min = 8, max = 100,
                message = "deve ter entre 8 e 100 caracteres") String senha,
        @NotNull(message = "obrigatório") Perfil perfil
) {
}
