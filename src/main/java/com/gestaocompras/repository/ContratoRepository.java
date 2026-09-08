package com.gestaocompras.repository;

import com.gestaocompras.model.Contrato;
import com.gestaocompras.model.StatusContrato;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ContratoRepository
        extends JpaRepository<Contrato, Long>, JpaSpecificationExecutor<Contrato> {

    @Override
    @EntityGraph(attributePaths = {"dotacao", "fornecedor", "licitacao"})
    Page<Contrato> findAll(Specification<Contrato> spec, Pageable pageable);

    boolean existsByNumeroAndOrganizacaoId(String numero, Long organizacaoId);

    boolean existsByFornecedorIdAndOrganizacaoIdAndStatusIn(Long fornecedorId,
            Long organizacaoId, Collection<StatusContrato> status);

    Optional<Contrato> findByNumeroAndOrganizacaoId(String numero, Long organizacaoId);

    Optional<Contrato> findByIdAndOrganizacaoId(Long id, Long organizacaoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Contrato c where c.id = :id and c.organizacao.id = :organizacaoId")
    Optional<Contrato> findByIdComLock(Long id, Long organizacaoId);
}