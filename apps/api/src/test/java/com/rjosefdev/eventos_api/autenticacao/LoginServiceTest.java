package com.rjosefdev.eventos_api.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.rjosefdev.eventos_api.autenticacao.TokenAcessoService.TokenAcessoGerado;
import com.rjosefdev.eventos_api.usuarios.Perfil;
import com.rjosefdev.eventos_api.usuarios.Usuario;
import com.rjosefdev.eventos_api.usuarios.UsuarioRepository;

class LoginServiceTest {

    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(8);
    private final TokenAcessoService tokenAcessoService = mock(TokenAcessoService.class);
    private final LoginService service = new LoginService(repository, encoder, tokenAcessoService);

    @Test
    void autenticaParticipanteComEmailNormalizadoERetornaContratoPublico() {
        Usuario usuario = usuario(Perfil.PARTICIPANTE, true, " senha segura ");
        when(repository.findByEmail("maria@exemplo.com")).thenReturn(Optional.of(usuario));
        when(tokenAcessoService.gerar(usuario)).thenReturn(
            new TokenAcessoGerado("jwt-participante", Instant.parse("2026-07-11T16:00:00Z"))
        );

        LoginResponse response = service.autenticar(
            new LoginRequest("  MARIA@EXEMPLO.COM  ", " senha segura ")
        );

        assertThat(response.tokenAcesso()).isEqualTo("jwt-participante");
        assertThat(response.tipoToken()).isEqualTo("Bearer");
        assertThat(response.expiraEm()).isEqualTo(Instant.parse("2026-07-11T16:00:00Z"));
        assertThat(response.usuario()).isEqualTo(new UsuarioResumoResponse(
            "usuario-PARTICIPANTE",
            "Usuário PARTICIPANTE",
            "usuario-participante@exemplo.com",
            Perfil.PARTICIPANTE
        ));
        verify(repository).findByEmail("maria@exemplo.com");
    }

    @Test
    void autenticaOrganizadorNoMesmoFluxo() {
        Usuario usuario = usuario(Perfil.ORGANIZADOR, true, " senha segura ");
        when(repository.findByEmail("organizador@exemplo.com")).thenReturn(Optional.of(usuario));
        when(tokenAcessoService.gerar(usuario)).thenReturn(
            new TokenAcessoGerado("jwt-organizador", Instant.parse("2026-07-11T16:00:00Z"))
        );

        LoginResponse response = service.autenticar(
            new LoginRequest("organizador@exemplo.com", " senha segura ")
        );

        assertThat(response.usuario().perfil()).isEqualTo(Perfil.ORGANIZADOR);
        assertThat(response.tokenAcesso()).isEqualTo("jwt-organizador");
    }

    @Test
    void rejeitaEmailInexistenteSenhaInvalidaEUsuarioInativoComMesmoErroGenerico() {
        Usuario ativo = usuario(Perfil.PARTICIPANTE, true, " senha correta ");
        Usuario inativo = usuario(Perfil.PARTICIPANTE, false, " senha correta ");
        when(repository.findByEmail("inexistente@exemplo.com")).thenReturn(Optional.empty());
        when(repository.findByEmail("ativo@exemplo.com")).thenReturn(Optional.of(ativo));
        when(repository.findByEmail("inativo@exemplo.com")).thenReturn(Optional.of(inativo));

        assertThatThrownBy(() -> service.autenticar(new LoginRequest("inexistente@exemplo.com", "senha qualquer")))
            .isInstanceOf(CredenciaisInvalidasException.class)
            .hasMessage("E-mail ou senha inválidos.");
        assertThatThrownBy(() -> service.autenticar(new LoginRequest("ativo@exemplo.com", "senha errada")))
            .isInstanceOf(CredenciaisInvalidasException.class)
            .hasMessage("E-mail ou senha inválidos.");
        assertThatThrownBy(() -> service.autenticar(new LoginRequest("inativo@exemplo.com", " senha correta ")))
            .isInstanceOf(CredenciaisInvalidasException.class)
            .hasMessage("E-mail ou senha inválidos.");
        verifyNoInteractions(tokenAcessoService);
    }

    private Usuario usuario(Perfil perfil, boolean ativo, String senha) {
        Usuario usuario = new Usuario(
            "Usuário " + perfil.name(),
            "usuario-" + perfil.name().toLowerCase() + "@exemplo.com",
            encoder.encode(senha),
            perfil,
            ativo,
            Instant.parse("2026-07-11T15:00:00Z")
        );
        usuario.setId("usuario-" + perfil.name());
        return usuario;
    }
}
