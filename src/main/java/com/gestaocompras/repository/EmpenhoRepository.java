package com.gestaocompras.repository;

import com.gestaocompras.model.Empenho;
import com.gestaocompras.model.StatusEmpenho;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmpenhoRepository
        extends JpaRepository<Empenho, Long>, JpaSpecificationExecutor<Empenho> {

    boolean existsByContratoIdAndAnoReferenciaAndMesReferenciaAndStatusIn(Long contratoId,
            Integer anoReferencia, Integer mesReferencia, List<StatusEmpenho> status);
}
