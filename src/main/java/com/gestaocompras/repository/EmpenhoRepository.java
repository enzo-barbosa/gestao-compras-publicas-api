package com.gestaocompras.repository;

import com.gestaocompras.model.Empenho;
import com.gestaocompras.model.StatusEmpenho;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmpenhoRepository
        extends JpaRepository<Empenho, Long>, JpaSpecificationExecutor<Empenho> {

    @Override
    @EntityGraph(attributePaths = {"contrato", "contrato.dotacao", "contrato.fornecedor", "usuario"})
    Page<Empenho> findAll(Specification<Empenho> spec, Pageable pageable);

    boolean existsByContratoIdAndAnoReferenciaAndMesReferenciaAndStatusIn(Long contratoId,
            Integer anoReferencia, Integer mesReferencia, List<StatusEmpenho> status);

    boolean existsByContratoIdAndStatusIn(Long contratoId, Collection<StatusEmpenho> status);
}
