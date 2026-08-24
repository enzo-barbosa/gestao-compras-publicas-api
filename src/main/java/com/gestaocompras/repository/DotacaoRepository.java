package com.gestaocompras.repository;

import com.gestaocompras.model.DotacaoOrcamentaria;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DotacaoRepository extends JpaRepository<DotacaoOrcamentaria, Long> {

    boolean existsByCodigo(String codigo);

    Optional<DotacaoOrcamentaria> findByCodigo(String codigo);

    Page<DotacaoOrcamentaria> findByAnoExercicio(Integer anoExercicio, Pageable pageable);
}
