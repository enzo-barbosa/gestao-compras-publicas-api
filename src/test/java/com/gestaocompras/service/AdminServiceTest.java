package com.gestaocompras.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gestaocompras.dto.AdminPerfilRequestDTO;
import com.gestaocompras.exception.NotFoundException;
import com.gestaocompras.exception.OperacaoNaoPermitidaException;
import com.gestaocompras.model.Organizacao;
import com.gestaocompras.model.Perfil;
import com.gestaocompras.model.Usuario;
import com.gestaocompras.repository.MembroOrganizacaoRepository;
import com.gestaocompras.repository.OrganizacaoRepository;
import com.gestaocompras.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    private static final String EMAIL_LOGADO = "super@admin.com";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private OrganizacaoRepository organizacaoRepository;

    @Mock
    private MembroOrganizacaoRepository membroRepository;

    @InjectMocks
    private AdminService adminService;

    private Usuario usuario(Long id, String nome, String email, Perfil perfil) {
        return Usuario.builder().id(id).nome(nome).email(email).perfil(perfil)
                .senha("x").build();
    }

    @Test
    void listaUsuariosOrdenadosPorNome() {
        Usuario ana = usuario(1L, "Ana", "ana@x.com", Perfil.USUARIO);
        Usuario ze = usuario(2L, "Zé", "ze@x.com", Perfil.SUPER_ADMIN);
        when(usuarioRepository.findAll(any(Sort.class))).thenReturn(List.of(ana, ze));

        var resposta = adminService.listarUsuarios();

        assertThat(resposta).hasSize(2);
        assertThat(resposta.get(0).nome()).isEqualTo("Ana");
        assertThat(resposta.get(1).perfil()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void alteraPerfilDeOutroUsuario() {
        Usuario alvo = usuario(3L, "Alvo", "alvo@x.com", Perfil.USUARIO);
        Usuario logado = usuario(9L, "Super", EMAIL_LOGADO, Perfil.SUPER_ADMIN);
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(alvo));
        when(usuarioRepository.findByEmail(EMAIL_LOGADO)).thenReturn(Optional.of(logado));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = adminService.alterarPerfil(3L,
                new AdminPerfilRequestDTO(Perfil.SUPER_ADMIN), EMAIL_LOGADO);

        assertThat(resposta.perfil()).isEqualTo("SUPER_ADMIN");
        assertThat(alvo.getPerfil()).isEqualTo(Perfil.SUPER_ADMIN);
        verify(usuarioRepository).save(alvo);
    }

    @Test
    void naoAlteraQuandoPerfilJaIgual() {
        Usuario alvo = usuario(4L, "Mesmo", "mesmo@x.com", Perfil.USUARIO);
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(alvo));

        var resposta = adminService.alterarPerfil(4L,
                new AdminPerfilRequestDTO(Perfil.USUARIO), EMAIL_LOGADO);

        assertThat(resposta.perfil()).isEqualTo("USUARIO");
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void impedeRebaixarProprioPerfil() {
        Usuario logado = usuario(5L, "Eu", EMAIL_LOGADO, Perfil.SUPER_ADMIN);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(logado));
        when(usuarioRepository.findByEmail(EMAIL_LOGADO)).thenReturn(Optional.of(logado));

        assertThatThrownBy(() -> adminService.alterarPerfil(5L,
                new AdminPerfilRequestDTO(Perfil.USUARIO), EMAIL_LOGADO))
                .isInstanceOf(OperacaoNaoPermitidaException.class)
                .hasMessageContaining("próprio perfil");
    }

    @Test
    void impedeRebaixarUnicoSuperAdmin() {
        Usuario alvo = usuario(6L, "Admin", "admin@x.com", Perfil.SUPER_ADMIN);
        Usuario logado = usuario(5L, "Eu", EMAIL_LOGADO, Perfil.SUPER_ADMIN);
        when(usuarioRepository.findById(6L)).thenReturn(Optional.of(alvo));
        when(usuarioRepository.findByEmail(EMAIL_LOGADO)).thenReturn(Optional.of(logado));
        when(usuarioRepository.countByPerfil(Perfil.SUPER_ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> adminService.alterarPerfil(6L,
                new AdminPerfilRequestDTO(Perfil.USUARIO), EMAIL_LOGADO))
                .isInstanceOf(OperacaoNaoPermitidaException.class)
                .hasMessageContaining("apenas um super administrador");
    }

    @Test
    void lancaLegitimamenteQuandoUsuarioNaoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.alterarPerfil(99L,
                new AdminPerfilRequestDTO(Perfil.USUARIO), EMAIL_LOGADO))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listaOrganizacoesComTotalDeMembros() {
        Organizacao prefeitura = Organizacao.builder().id(1L).nome("Prefeitura")
                .criadoEm(LocalDateTime.now()).build();
        Organizacao camara = Organizacao.builder().id(2L).nome("Câmara")
                .criadoEm(LocalDateTime.now()).build();
        when(organizacaoRepository.findAll(any(Sort.class))).thenReturn(List.of(prefeitura, camara));
        when(membroRepository.findByIdOrganizacaoId(1L))
                .thenReturn(List.of(mockMembro(), mockMembro()));
        when(membroRepository.findByIdOrganizacaoId(2L)).thenReturn(List.of(mockMembro()));

        var resposta = adminService.listarOrganizacoes();

        assertThat(resposta).hasSize(2);
        assertThat(resposta.get(0).totalMembros()).isEqualTo(2L);
        assertThat(resposta.get(1).totalMembros()).isEqualTo(1L);
    }

    private com.gestaocompras.model.MembroOrganizacao mockMembro() {
        return org.mockito.Mockito.mock(com.gestaocompras.model.MembroOrganizacao.class);
    }
}