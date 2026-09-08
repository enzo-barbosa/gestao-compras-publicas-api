package com.gestaocompras.repository;

import com.gestaocompras.model.Fornecedor;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    Page<Fornecedor> findByOrganizacaoId(Long organizacaoId, Pageable pageable);

    Page<Fornecedor> findByNomeContainingIgnoreCaseAndOrganizacaoId(String nome,
            Long organizacaoId, Pageable pageable);

    Optional<Fornecedor> findByIdAndOrganizacaoId(Long id, Long organizacaoId);

    Optional<Fornecedor> findByCnpjAndOrganizacaoId(String cnpj, Long organizacaoId);

    boolean existsByCnpjAndOrganizacaoId(String cnpj, Long organizacaoId);
}