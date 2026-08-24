package com.gestaocompras.repository;

import com.gestaocompras.model.Empenho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmpenhoRepository
        extends JpaRepository<Empenho, Long>, JpaSpecificationExecutor<Empenho> {

    boolean existsByContratoIdAndAnoReferenciaAndMesReferencia(Long contratoId,
            Integer anoReferencia, Integer mesReferencia);
}
