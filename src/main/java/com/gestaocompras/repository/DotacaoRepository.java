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

    Page<DotacaoOrcamentaria> findByOrganizacaoId(Long organizacaoId, Pageable pageable);

    Page<DotacaoOrcamentaria> findByAnoExercicioAndOrganizacaoId(Integer anoExercicio,
            Long organizacaoId, Pageable pageable);

    Optional<DotacaoOrcamentaria> findByIdAndOrganizacaoId(Long id, Long organizacaoId);

    Optional<DotacaoOrcamentaria> findByCodigoAndOrganizacaoId(String codigo, Long organizacaoId);

    boolean existsByCodigoAndOrganizacaoId(String codigo, Long organizacaoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DotacaoOrcamentaria d where d.id = :id and d.organizacao.id = :organizacaoId")
    Optional<DotacaoOrcamentaria> findByIdComLock(Long id, Long organizacaoId);
}