package com.gestaocompras.repository;

import com.gestaocompras.model.CreditoSuplementar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CreditoSuplementarRepository
        extends JpaRepository<CreditoSuplementar, Long>, JpaSpecificationExecutor<CreditoSuplementar> {

    @Override
    @EntityGraph(attributePaths = {"dotacaoOrigem", "dotacaoDestino"})
    Page<CreditoSuplementar> findAll(Specification<CreditoSuplementar> spec, Pageable pageable);
}
