package com.gestaocompras.repository;

import com.gestaocompras.model.DotacaoOrcamentaria;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface DotacaoRepository extends JpaRepository<DotacaoOrcamentaria, Long> {

    boolean existsByCodigo(String codigo);

    Optional<DotacaoOrcamentaria> findByCodigo(String codigo);

    Page<DotacaoOrcamentaria> findByAnoExercicio(Integer anoExercicio, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DotacaoOrcamentaria d where d.id = :id")
    Optional<DotacaoOrcamentaria> findByIdComLock(Long id);
}
