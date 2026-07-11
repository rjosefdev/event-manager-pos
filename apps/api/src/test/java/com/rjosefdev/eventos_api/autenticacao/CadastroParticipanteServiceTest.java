package com.rjosefdev.eventos_api.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.rjosefdev.eventos_api.usuarios.Perfil;
import com.rjosefdev.eventos_api.usuarios.Usuario;
import com.rjosefdev.eventos_api.usuarios.UsuarioRepository;

class CadastroParticipanteServiceTest {

    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(8);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-11T15:00:00Z"), ZoneOffset.UTC);
    private final CadastroParticipanteService service = new CadastroParticipanteService(repository, encoder, clock);

    @Test
    void cadastraParticipanteComEmailNormalizadoESenhaBcryptDeCustoOito() {
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId("usuario-1");
            return usuario;
        });

        ParticipanteResponse response = service.cadastrar(
            new CadastroParticipanteRequest("  Maria Silva  ", "  MARIA@EXEMPLO.COM  ", " senha segura ")
        );

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(usuarioCaptor.capture());
        Usuario persistido = usuarioCaptor.getValue();

        assertThat(persistido.getNome()).isEqualTo("Maria Silva");
        assertThat(persistido.getEmail()).isEqualTo("maria@exemplo.com");
        assertThat(persistido.getPerfil()).isEqualTo(Perfil.PARTICIPANTE);
        assertThat(persistido.isAtivo()).isTrue();
        assertThat(persistido.getSenhaHash()).startsWith("$2a$08$").isNotEqualTo(" senha segura ");
        assertThat(encoder.matches(" senha segura ", persistido.getSenhaHash())).isTrue();
        assertThat(response).isEqualTo(
            new ParticipanteResponse("usuario-1", "Maria Silva", "maria@exemplo.com", Perfil.PARTICIPANTE)
        );
    }

    @Test
    void converteColisaoDoIndiceEmErroDeDominio() {
        when(repository.save(any(Usuario.class))).thenThrow(new DuplicateKeyException("uk_usuarios_email"));

        assertThatThrownBy(() -> service.cadastrar(
            new CadastroParticipanteRequest("Maria", "maria@exemplo.com", "12345678")
        )).isInstanceOf(EmailJaCadastradoException.class);
    }
}
