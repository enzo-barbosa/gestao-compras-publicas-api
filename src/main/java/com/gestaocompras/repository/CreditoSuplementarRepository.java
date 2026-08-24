package com.gestaocompras.repository;

import com.gestaocompras.model.CreditoSuplementar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CreditoSuplementarRepository
        extends JpaRepository<CreditoSuplementar, Long>, JpaSpecificationExecutor<CreditoSuplementar> {
}
