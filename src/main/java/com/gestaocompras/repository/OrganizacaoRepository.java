package com.gestaocompras.repository;

import com.gestaocompras.model.Organizacao;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizacaoRepository extends JpaRepository<Organizacao, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    Optional<Organizacao> findByCodigoAcesso(String codigoAcesso);
}