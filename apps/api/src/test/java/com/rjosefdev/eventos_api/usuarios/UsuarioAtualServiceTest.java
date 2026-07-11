package com.rjosefdev.eventos_api.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import com.rjosefdev.eventos_api.autenticacao.UsuarioResumoResponse;

class UsuarioAtualServiceTest {

    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final UsuarioAtualService service = new UsuarioAtualService(repository);

    @Test
    void buscaUsuarioAtualExclusivamentePeloSubDoToken() {
        Usuario usuario = usuario(Perfil.PARTICIPANTE);
        when(repository.findById("usuario-sub")).thenReturn(Optional.of(usuario));

        UsuarioResumoResponse response = service.buscarPorSub("usuario-sub");

        assertThat(response).isEqualTo(new UsuarioResumoResponse(
            "usuario-sub",
            "Maria",
            "maria@exemplo.com",
            Perfil.PARTICIPANTE
        ));
    }

    @Test
    void rejeitaSubSemUsuarioCorrespondente() {
        when(repository.findById("usuario-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorSub("usuario-inexistente"))
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Usuário do token não encontrado.");
    }

    private Usuario usuario(Perfil perfil) {
        Usuario usuario = new Usuario(
            "Maria",
            "maria@exemplo.com",
            "hash",
            perfil,
            true,
            Instant.parse("2026-07-11T15:00:00Z")
        );
        usuario.setId("usuario-sub");
        return usuario;
    }
}
