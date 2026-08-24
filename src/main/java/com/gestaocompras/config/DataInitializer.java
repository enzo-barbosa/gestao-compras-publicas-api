package com.gestaocompras.config;

import com.gestaocompras.model.Perfil;
import com.gestaocompras.model.Usuario;
import com.gestaocompras.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String ADMIN_EMAIL = "admin@admin.com";

    @Bean
    CommandLineRunner seedAdmin(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (usuarioRepository.existsByEmail(ADMIN_EMAIL)) {
                log.info("Usuário administrador já existe (email: {})", ADMIN_EMAIL);
                return;
            }
            Usuario admin = Usuario.builder()
                    .nome("Administrador")
                    .email(ADMIN_EMAIL)
                    .senha(passwordEncoder.encode("admin"))
                    .perfil(Perfil.ADMIN)
                    .build();
            usuarioRepository.save(admin);
            log.info("Usuário administrador criado (email: {})", ADMIN_EMAIL);
        };
    }
}
