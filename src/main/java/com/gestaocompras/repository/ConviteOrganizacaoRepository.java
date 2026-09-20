package com.gestaocompras.repository;

import com.gestaocompras.model.ConviteOrganizacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ConviteOrganizacaoRepository
        extends JpaRepository<ConviteOrganizacao, Long> {

    Optional<ConviteOrganizacao> findByEmailAndUsadoEmIsNullAndRecusadoEmIsNullAndOrganizacaoId(
            String email, Long organizacaoId);

    List<ConviteOrganizacao> findByOrganizacaoIdAndUsadoEmIsNull(Long organizacaoId);

    Optional<ConviteOrganizacao> findByIdAndEmailAndUsadoEmIsNullAndRecusadoEmIsNull(Long id,
            String email);

    Optional<ConviteOrganizacao> findByIdAndEmailAndUsadoEmIsNull(Long id, String email);

    List<ConviteOrganizacao> findByEmailAndUsadoEmIsNullAndRecusadoEmIsNull(String email);

    @Modifying
    @Query("delete from ConviteOrganizacao c where c.organizacao.id = :organizacaoId")
    void deleteByOrganizacaoId(Long organizacaoId);
}