package com.gestaocompras.dto;

import com.gestaocompras.model.Fornecedor;

public record FornecedorResponseDTO(
        Long id,
        String nome,
        String cnpj,
        String email,
        String telefone,
        String endereco
) {

    public static FornecedorResponseDTO from(Fornecedor fornecedor) {
        return new FornecedorResponseDTO(fornecedor.getId(), fornecedor.getNome(), fornecedor.getCnpj(),
                fornecedor.getEmail(), fornecedor.getTelefone(), fornecedor.getEndereco());
    }
}
