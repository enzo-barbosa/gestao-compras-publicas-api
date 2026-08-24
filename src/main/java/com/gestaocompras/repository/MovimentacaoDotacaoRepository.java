package com.gestaocompras.repository;

import com.gestaocompras.model.MovimentacaoDotacao;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoDotacaoRepository extends JpaRepository<MovimentacaoDotacao, Long> {

    Page<MovimentacaoDotacao> findByDotacaoId(Long dotacaoId, Pageable pageable);

    List<MovimentacaoDotacao> findAllByDotacaoId(Long dotacaoId);

    long countByDotacaoId(Long dotacaoId);
}
