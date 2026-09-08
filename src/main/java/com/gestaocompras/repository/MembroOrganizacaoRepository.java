package com.gestaocompras.repository;

import com.gestaocompras.model.MembroOrganizacao;
import com.gestaocompras.model.PapelOrganizacao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembroOrganizacaoRepository
        extends JpaRepository<MembroOrganizacao, MembroOrganizacao.Id> {

    Optional<MembroOrganizacao> findByIdOrganizacaoIdAndIdUsuarioId(Long organizacaoId,
            Long usuarioId);

    List<MembroOrganizacao> findByIdOrganizacaoId(Long organizacaoId);

    List<MembroOrganizacao> findByIdUsuarioId(Long usuarioId);

    boolean existsByIdOrganizacaoIdAndIdUsuarioId(Long organizacaoId, Long usuarioId);

    boolean existsByIdOrganizacaoIdAndPapel(Long organizacaoId, PapelOrganizacao papel);
}