package com.gestaocompras.dto;

import com.gestaocompras.validation.Cnpj;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FornecedorRequestDTO(
        @NotBlank(message = "obrigatório") @Size(max = 150) String nome,
        @NotBlank(message = "obrigatório") @Cnpj String cnpj,
        @Email(message = "deve ser um e-mail válido") @Size(max = 150) String email,
        @Size(max = 30) String telefone,
        @Size(max = 250) String endereco
) {
}
