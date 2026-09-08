package com.gestaocompras.repository;

import com.gestaocompras.model.Perfil;
import com.gestaocompras.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByPerfil(Perfil perfil);
}
