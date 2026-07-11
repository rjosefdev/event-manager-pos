package com.rjosefdev.eventos_api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.rjosefdev.eventos_api.autenticacao.EmailJaCadastradoException;
import com.rjosefdev.eventos_api.usuarios.Perfil;
import com.rjosefdev.eventos_api.usuarios.Usuario;
import com.rjosefdev.eventos_api.usuarios.UsuarioRepository;

class AdminServiceTest {

    private static final Instant AGORA = Instant.parse("2026-07-11T15:00:00Z");

    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(8);
    private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);
    private final AdminService service = new AdminService(repository, encoder, clock);

    @Test
    void cadastraOrganizadorComEmailNormalizadoESenhaProtegida() {
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId("organizador-1");
            return usuario;
        });

        UsuarioAdminResponse response = service.cadastrarOrganizador(
            new CadastroOrganizadorRequest(
                "  Organização Exemplo  ",
                "  ORGANIZADOR@EXEMPLO.COM  ",
                " senha segura "
            )
        );

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(usuarioCaptor.capture());
        Usuario persistido = usuarioCaptor.getValue();

        assertThat(persistido.getNome()).isEqualTo("Organização Exemplo");
        assertThat(persistido.getEmail()).isEqualTo("organizador@exemplo.com");
        assertThat(persistido.getPerfil()).isEqualTo(Perfil.ORGANIZADOR);
        assertThat(persistido.isAtivo()).isTrue();
        assertThat(encoder.matches(" senha segura ", persistido.getSenhaHash())).isTrue();
        assertThat(response.id()).isEqualTo("organizador-1");
        assertThat(response.perfil()).isEqualTo(Perfil.ORGANIZADOR);
    }

    @Test
    void converteEmailDuplicadoEmErroDeDominio() {
        when(repository.save(any(Usuario.class))).thenThrow(new DuplicateKeyException("uk_usuarios_email"));

        assertThatThrownBy(() -> service.cadastrarOrganizador(
            new CadastroOrganizadorRequest("Organizador", "organizador@exemplo.com", "12345678")
        )).isInstanceOf(EmailJaCadastradoException.class);
    }

    @Test
    void listaTodosOsUsuariosSemExporSenhaHash() {
        Usuario organizador = new Usuario(
            "Organizador",
            "organizador@exemplo.com",
            "hash-organizador",
            Perfil.ORGANIZADOR,
            true,
            AGORA
        );
        organizador.setId("usuario-1");
        Usuario participante = new Usuario(
            "Participante",
            "participante@exemplo.com",
            "hash-participante",
            Perfil.PARTICIPANTE,
            true,
            AGORA
        );
        participante.setId("usuario-2");
        when(repository.findAll()).thenReturn(List.of(organizador, participante));

        List<UsuarioAdminResponse> response = service.listarUsuarios();

        assertThat(response).extracting(UsuarioAdminResponse::id)
            .containsExactly("usuario-1", "usuario-2");
        assertThat(response).extracting(UsuarioAdminResponse::perfil)
            .containsExactly(Perfil.ORGANIZADOR, Perfil.PARTICIPANTE);
    }
}
