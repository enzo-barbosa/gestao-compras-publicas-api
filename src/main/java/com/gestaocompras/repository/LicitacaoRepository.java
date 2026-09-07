package com.gestaocompras.repository;

import com.gestaocompras.model.Licitacao;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface LicitacaoRepository
        extends JpaRepository<Licitacao, Long>, JpaSpecificationExecutor<Licitacao> {

    boolean existsByNumeroEdital(String numeroEdital);

    boolean existsByVencedorId(Long fornecedorId);

    Optional<Licitacao> findByNumeroEdital(String numeroEdital);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Licitacao l where l.id = :id")
    Optional<Licitacao> findByIdComLock(Long id);
}
