package com.gestaocompras.dto;

import com.gestaocompras.model.Organizacao;
import java.time.LocalDateTime;

public record OrganizacaoResponseDTO(
        Long id,
        String nome,
        LocalDateTime criadoEm,
        String papel
) {

    public static OrganizacaoResponseDTO from(Organizacao organizacao, String papel) {
        return new OrganizacaoResponseDTO(organizacao.getId(), organizacao.getNome(),
                organizacao.getCriadoEm(), papel);
    }
}