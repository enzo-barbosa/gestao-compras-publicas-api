package com.gestaocompras.repository;

import com.gestaocompras.model.EmpenhoSequencia;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EmpenhoSequenciaRepository
        extends JpaRepository<EmpenhoSequencia, EmpenhoSequencia.EmpenhoSequenciaId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from EmpenhoSequencia s "
            + "where s.organizacaoId = :organizacaoId and s.anoReferencia = :anoReferencia")
    Optional<EmpenhoSequencia> findByIdComLock(Long organizacaoId, Integer anoReferencia);

    @Modifying
    @Query("delete from EmpenhoSequencia s where s.organizacaoId = :organizacaoId")
    void deleteByOrganizacaoId(Long organizacaoId);
}