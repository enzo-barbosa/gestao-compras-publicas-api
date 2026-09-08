package com.gestaocompras.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal autenticado de cada requisição.
 * Carrega a organização ativa (header {@code X-Org-Id}) e o papel dentro dela,
 * além da flag de super admin global.
 */
public class UsuarioLogado implements UserDetails {

    private final String email;
    private final Long organizacaoId;
    private final String papel;
    private final boolean superAdmin;

    public UsuarioLogado(String email, Long organizacaoId, String papel,
            boolean superAdmin) {
        this.email = email;
        this.organizacaoId = organizacaoId;
        this.papel = papel;
        this.superAdmin = superAdmin;
    }

    public static UsuarioLogado superAdmin(String email, Long organizacaoId) {
        return new UsuarioLogado(email, organizacaoId, null, true);
    }

    public static UsuarioLogado membro(String email, Long organizacaoId, String papel) {
        return new UsuarioLogado(email, organizacaoId, papel, false);
    }

    public static UsuarioLogado semOrganizacao(String email) {
        return new UsuarioLogado(email, null, null, false);
    }

    public Long organizacaoId() {
        return organizacaoId;
    }

    public boolean ehSuperAdmin() {
        return superAdmin;
    }

    public String papel() {
        return papel;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (superAdmin) {
            return List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }
        return papel == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + papel));
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }
}