package com.gestaocompras.repository;

import com.gestaocompras.model.RecuperacaoSenhaToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecuperacaoSenhaTokenRepository
        extends JpaRepository<RecuperacaoSenhaToken, Long> {

    Optional<RecuperacaoSenhaToken> findByUsuarioIdAndUsadoEmIsNull(Long usuarioId);
}