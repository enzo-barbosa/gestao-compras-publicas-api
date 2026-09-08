package com.gestaocompras.repository;

import com.gestaocompras.model.ConviteOrganizacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConviteOrganizacaoRepository
        extends JpaRepository<ConviteOrganizacao, Long> {

    Optional<ConviteOrganizacao> findByEmailAndUsadoEmIsNull(String email);

    Optional<ConviteOrganizacao> findByEmailAndUsadoEmIsNullAndOrganizacaoId(String email,
            Long organizacaoId);

    Optional<ConviteOrganizacao> findByCodigoAndUsadoEmIsNull(String codigo);

    List<ConviteOrganizacao> findByOrganizacaoIdAndUsadoEmIsNull(Long organizacaoId);

    List<ConviteOrganizacao> findAllByEmailAndUsadoEmIsNull(String email);
}