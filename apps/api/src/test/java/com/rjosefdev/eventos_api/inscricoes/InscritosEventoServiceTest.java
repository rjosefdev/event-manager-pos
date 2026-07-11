package com.rjosefdev.eventos_api.inscricoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.rjosefdev.eventos_api.eventos.Evento;
import com.rjosefdev.eventos_api.eventos.EventoRepository;
import com.rjosefdev.eventos_api.eventos.RecursoNaoEncontradoException;
import com.rjosefdev.eventos_api.usuarios.Perfil;
import com.rjosefdev.eventos_api.usuarios.Usuario;
import com.rjosefdev.eventos_api.usuarios.UsuarioRepository;

class InscritosEventoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-07-11T15:00:00Z");

    private final EventoRepository eventoRepository = mock(EventoRepository.class);
    private final InscricaoRepository inscricaoRepository = mock(InscricaoRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final InscritosEventoService service = new InscritosEventoService(
        eventoRepository,
        inscricaoRepository,
        usuarioRepository
    );

    @Test
    void listaVaziaQuandoEventoProprioNaoTemInscricoes() {
        Evento evento = evento("evento-1", "organizador-1");
        when(eventoRepository.findByIdAndOrganizadorId("evento-1", "organizador-1")).thenReturn(Optional.of(evento));
        when(inscricaoRepository.findByEventoIdOrderByInscritoEmDesc("evento-1")).thenReturn(List.of());
        when(usuarioRepository.findAllById(List.of())).thenReturn(List.of());

        List<InscritoEventoResponse> inscritos = service.listarDoEvento("organizador-1", "evento-1");

        assertThat(inscritos).isEmpty();
        verify(eventoRepository).findByIdAndOrganizadorId("evento-1", "organizador-1");
        verify(inscricaoRepository).findByEventoIdOrderByInscritoEmDesc("evento-1");
    }

    @Test
    void listaMultiplosInscritosComSituacoesDiferentesEDadosPublicosDoParticipante() {
        Evento evento = evento("evento-1", "organizador-1");
        Inscricao ativa = inscricao("inscricao-1", "participante-1", SituacaoInscricao.ATIVA);
        Inscricao cancelada = inscricao("inscricao-2", "participante-2", SituacaoInscricao.ATIVA);
        cancelada.cancelar(AGORA.minusSeconds(60));
        Usuario participante1 = usuario("participante-1", "Ana Lima", "ana@email.com");
        Usuario participante2 = usuario("participante-2", "Bruno Dias", "bruno@email.com");

        when(eventoRepository.findByIdAndOrganizadorId("evento-1", "organizador-1")).thenReturn(Optional.of(evento));
        when(inscricaoRepository.findByEventoIdOrderByInscritoEmDesc("evento-1")).thenReturn(List.of(ativa, cancelada));
        when(usuarioRepository.findAllById(List.of("participante-1", "participante-2")))
            .thenReturn(List.of(participante1, participante2));

        List<InscritoEventoResponse> inscritos = service.listarDoEvento("organizador-1", "evento-1");

        assertThat(inscritos).extracting(InscritoEventoResponse::id).containsExactly("inscricao-1", "inscricao-2");
        assertThat(inscritos).extracting(InscritoEventoResponse::eventoId).containsOnly("evento-1");
        assertThat(inscritos).extracting(InscritoEventoResponse::situacao)
            .containsExactly(SituacaoInscricao.ATIVA, SituacaoInscricao.CANCELADA);
        assertThat(inscritos.get(0).participante().nome()).isEqualTo("Ana Lima");
        assertThat(inscritos.get(0).participante().email()).isEqualTo("ana@email.com");
        assertThat(inscritos.get(1).canceladoEm()).isEqualTo(AGORA.minusSeconds(60));
    }

    @Test
    void outroOrganizadorRecebeMesmoNaoEncontradoDeEventoInexistente() {
        when(eventoRepository.findByIdAndOrganizadorId("evento-1", "organizador-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarDoEvento("organizador-2", "evento-1"))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessage("Recurso não encontrado.");

        verify(inscricaoRepository, never()).findByEventoIdOrderByInscritoEmDesc("evento-1");
        verify(usuarioRepository, never()).findAllById(List.of());
    }

    @Test
    void participanteDaInscricaoAusenteRetornaNaoEncontradoSemExporDadoInconsistente() {
        Evento evento = evento("evento-1", "organizador-1");
        Inscricao inscricao = inscricao("inscricao-1", "participante-ausente", SituacaoInscricao.ATIVA);
        when(eventoRepository.findByIdAndOrganizadorId("evento-1", "organizador-1")).thenReturn(Optional.of(evento));
        when(inscricaoRepository.findByEventoIdOrderByInscritoEmDesc("evento-1")).thenReturn(List.of(inscricao));
        when(usuarioRepository.findAllById(List.of("participante-ausente"))).thenReturn(List.of());

        assertThatThrownBy(() -> service.listarDoEvento("organizador-1", "evento-1"))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessage("Recurso não encontrado.");
    }

    private Evento evento(String id, String organizadorId) {
        Evento evento = new Evento(
            organizadorId,
            "Backend Day",
            "Palestras sobre Java e Spring.",
            Instant.parse("2026-07-12T15:00:00Z"),
            Instant.parse("2026-07-12T18:00:00Z"),
            "Auditório 1",
            false,
            "Tecnologia",
            80,
            null,
            AGORA.minusSeconds(3600)
        );
        evento.setId(id);
        return evento;
    }

    private Inscricao inscricao(String id, String participanteId, SituacaoInscricao situacao) {
        Inscricao inscricao = new Inscricao(
            "evento-1",
            participanteId,
            situacao,
            AGORA.minusSeconds(600),
            null,
            AGORA.minusSeconds(600),
            AGORA.minusSeconds(600)
        );
        inscricao.setId(id);
        return inscricao;
    }

    private Usuario usuario(String id, String nome, String email) {
        Usuario usuario = new Usuario(nome, email, "$2a$08$hash", Perfil.PARTICIPANTE, true, AGORA.minusSeconds(7200));
        usuario.setId(id);
        return usuario;
    }
}
