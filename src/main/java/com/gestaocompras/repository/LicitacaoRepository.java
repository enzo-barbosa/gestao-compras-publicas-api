package com.gestaocompras.repository;

import com.gestaocompras.model.Licitacao;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface LicitacaoRepository
        extends JpaRepository<Licitacao, Long>, JpaSpecificationExecutor<Licitacao> {

    @Override
    @EntityGraph(attributePaths = {"vencedor"})
    Page<Licitacao> findAll(Specification<Licitacao> spec, Pageable pageable);

    boolean existsByNumeroEdital(String numeroEdital);

    boolean existsByVencedorId(Long fornecedorId);

    Optional<Licitacao> findByNumeroEdital(String numeroEdital);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Licitacao l where l.id = :id")
    Optional<Licitacao> findByIdComLock(Long id);
}
