package com.gestaocompras.repository;

import com.gestaocompras.model.Licitacao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LicitacaoRepository
        extends JpaRepository<Licitacao, Long>, JpaSpecificationExecutor<Licitacao> {

    boolean existsByNumeroEdital(String numeroEdital);

    Optional<Licitacao> findByNumeroEdital(String numeroEdital);
}
