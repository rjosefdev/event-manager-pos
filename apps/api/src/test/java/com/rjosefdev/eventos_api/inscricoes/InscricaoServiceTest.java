package com.rjosefdev.eventos_api.inscricoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import com.rjosefdev.eventos_api.eventos.Evento;
import com.rjosefdev.eventos_api.eventos.EventoRepository;
import com.rjosefdev.eventos_api.eventos.RecursoNaoEncontradoException;

class InscricaoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-07-11T15:00:00Z");

    private final InscricaoRepository inscricaoRepository = mock(InscricaoRepository.class);
    private final EventoRepository eventoRepository = mock(EventoRepository.class);
    private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);
    private final InscricaoService service = new InscricaoService(inscricaoRepository, eventoRepository, clock);

    @Test
    void criaInscricaoDerivandoParticipanteIdDoSubDoJwt() {
        Evento evento = evento("evento-1", 10, Instant.parse("2026-07-12T15:00:00Z"), Instant.parse("2026-07-12T18:00:00Z"));
        when(eventoRepository.findById("evento-1")).thenReturn(Optional.of(evento));
        when(inscricaoRepository.countByEventoIdAndSituacao("evento-1", SituacaoInscricao.ATIVA)).thenReturn(3L);
        when(inscricaoRepository.findByEventoIdAndParticipanteId("evento-1", "participante-1")).thenReturn(Optional.empty());
        when(inscricaoRepository.save(any(Inscricao.class))).thenAnswer(invocation -> {
            Inscricao inscricao = invocation.getArgument(0);
            inscricao.setId("inscricao-1");
            return inscricao;
        });

        ResultadoCriacaoInscricao resultado = service.criar("participante-1", new CriarInscricaoRequest("evento-1"));
        InscricaoResponse response = resultado.inscricao();

        ArgumentCaptor<Inscricao> inscricaoCaptor = ArgumentCaptor.forClass(Inscricao.class);
        verify(inscricaoRepository).save(inscricaoCaptor.capture());
        Inscricao persistida = inscricaoCaptor.getValue();

        assertThat(persistida.getEventoId()).isEqualTo("evento-1");
        assertThat(persistida.getParticipanteId()).isEqualTo("participante-1");
        assertThat(persistida.getSituacao()).isEqualTo(SituacaoInscricao.ATIVA);
        assertThat(persistida.getInscritoEm()).isEqualTo(AGORA);
        assertThat(persistida.getCanceladoEm()).isNull();
        assertThat(response.id()).isEqualTo("inscricao-1");
        assertThat(response.participanteId()).isEqualTo("participante-1");
        assertThat(response.evento().vagasDisponiveis()).isEqualTo(7);
        assertThat(resultado.criada()).isTrue();
    }

    @Test
    void listaSomenteInscricoesDoParticipanteInformado() {
        Inscricao ativa = inscricao("inscricao-1", "evento-1", "participante-1", SituacaoInscricao.ATIVA);
        Inscricao cancelada = inscricao("inscricao-2", "evento-2", "participante-1", SituacaoInscricao.CANCELADA);
        cancelada.cancelar(AGORA.minusSeconds(60));
        when(inscricaoRepository.findByParticipanteIdOrderByInscritoEmDesc("participante-1"))
            .thenReturn(List.of(ativa, cancelada));
        when(eventoRepository.findById("evento-1")).thenReturn(Optional.of(evento("evento-1", 20,
            Instant.parse("2026-07-12T15:00:00Z"), Instant.parse("2026-07-12T18:00:00Z"))));
        when(eventoRepository.findById("evento-2")).thenReturn(Optional.of(evento("evento-2", 30,
            Instant.parse("2026-07-13T15:00:00Z"), Instant.parse("2026-07-13T18:00:00Z"))));
        when(inscricaoRepository.countByEventoIdAndSituacao("evento-1", SituacaoInscricao.ATIVA)).thenReturn(4L);
        when(inscricaoRepository.countByEventoIdAndSituacao("evento-2", SituacaoInscricao.ATIVA)).thenReturn(0L);

        List<InscricaoResponse> inscricoes = service.listar("participante-1");

        assertThat(inscricoes).extracting(InscricaoResponse::id).containsExactly("inscricao-1", "inscricao-2");
        assertThat(inscricoes).extracting(InscricaoResponse::participanteId).containsOnly("participante-1");
        verify(inscricaoRepository).findByParticipanteIdOrderByInscritoEmDesc("participante-1");
    }

    @Test
    void rejeitaCriacaoQuandoEventoEstaLotadoFinalizadoOuCancelado() {
        Evento lotado = evento("evento-lotado", 2, Instant.parse("2026-07-12T15:00:00Z"), Instant.parse("2026-07-12T18:00:00Z"));
        Evento finalizado = evento("evento-finalizado", 10, Instant.parse("2026-07-10T15:00:00Z"), Instant.parse("2026-07-10T18:00:00Z"));
        Evento cancelado = evento("evento-cancelado", 10, Instant.parse("2026-07-12T15:00:00Z"), Instant.parse("2026-07-12T18:00:00Z"));
        cancelado.cancelar(AGORA.minusSeconds(60));

        when(eventoRepository.findById("evento-lotado")).thenReturn(Optional.of(lotado));
        when(eventoRepository.findById("evento-finalizado")).thenReturn(Optional.of(finalizado));
        when(eventoRepository.findById("evento-cancelado")).thenReturn(Optional.of(cancelado));
        when(inscricaoRepository.countByEventoIdAndSituacao("evento-lotado", SituacaoInscricao.ATIVA)).thenReturn(2L);

        assertThatThrownBy(() -> service.criar("participante-1", new CriarInscricaoRequest("evento-lotado")))
            .isInstanceOf(InscricaoNaoPermitidaException.class)
            .hasMessage("Não há vagas disponíveis para este Evento.");
        assertThatThrownBy(() -> service.criar("participante-1", new CriarInscricaoRequest("evento-finalizado")))
            .isInstanceOf(InscricaoNaoPermitidaException.class)
            .hasMessage("Inscrições não podem ser alteradas depois do término do Evento.");
        assertThatThrownBy(() -> service.criar("participante-1", new CriarInscricaoRequest("evento-cancelado")))
            .isInstanceOf(InscricaoNaoPermitidaException.class)
            .hasMessage("Inscrições não podem ser alteradas em Evento cancelado.");

        verify(inscricaoRepository, never()).save(any());
    }

    @Test
    void cancelaInscricaoPropriaAntesDoTerminoDoEvento() {
        Inscricao inscricao = inscricao("inscricao-1", "evento-1", "participante-1", SituacaoInscricao.ATIVA);
        Evento evento = evento("evento-1", 10, Instant.parse("2026-07-11T14:00:00Z"), Instant.parse("2026-07-11T16:00:00Z"));
        when(inscricaoRepository.findByIdAndParticipanteId("inscricao-1", "participante-1")).thenReturn(Optional.of(inscricao));
        when(eventoRepository.findById("evento-1")).thenReturn(Optional.of(evento));
        when(inscricaoRepository.countByEventoIdAndSituacao("evento-1", SituacaoInscricao.ATIVA)).thenReturn(4L);
        when(inscricaoRepository.save(any(Inscricao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InscricaoResponse response = service.cancelar("participante-1", "inscricao-1");

        assertThat(response.situacao()).isEqualTo(SituacaoInscricao.CANCELADA);
        assertThat(response.canceladoEm()).isEqualTo(AGORA);
        verify(inscricaoRepository).save(inscricao);
    }

    @Test
    void reativaInscricaoCanceladaReutilizandoDocumentoEOcupandoVaga() {
        Inscricao inscricao = inscricao("inscricao-1", "evento-1", "participante-1", SituacaoInscricao.ATIVA);
        inscricao.cancelar(AGORA.minusSeconds(120));
        Evento evento = evento("evento-1", 10, Instant.parse("2026-07-12T15:00:00Z"), Instant.parse("2026-07-12T18:00:00Z"));
        when(inscricaoRepository.findByIdAndParticipanteId("inscricao-1", "participante-1")).thenReturn(Optional.of(inscricao));
        when(eventoRepository.findById("evento-1")).thenReturn(Optional.of(evento));
        when(inscricaoRepository.countByEventoIdAndSituacao("evento-1", SituacaoInscricao.ATIVA)).thenReturn(9L, 10L);
        when(inscricaoRepository.save(any(Inscricao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InscricaoResponse response = service.reativar("participante-1", "inscricao-1");

        assertThat(response.id()).isEqualTo("inscricao-1");
        assertThat(response.situacao()).isEqualTo(SituacaoInscricao.ATIVA);
        assertThat(response.canceladoEm()).isNull();
        assertThat(response.evento().vagasDisponiveis()).isZero();
        verify(inscricaoRepository).save(inscricao);
    }

    @Test
    void postDeInscricaoExistenteAtivaReutilizaDocumentoSemCriarOutro() {
        Evento evento = evento("evento-1", 10, Instant.parse("2026-07-12T15:00:00Z"), Instant.parse("2026-07-12T18:00:00Z"));
        Inscricao inscricao = inscricao("inscricao-1", "evento-1", "participante-1", SituacaoInscricao.ATIVA);
        when(eventoRepository.findById("evento-1")).thenReturn(Optional.of(evento));
        when(inscricaoRepository.countByEventoIdAndSituacao("evento-1", SituacaoInscricao.ATIVA)).thenReturn(3L);
        when(inscricaoRepository.findByEventoIdAndParticipanteId("evento-1", "participante-1")).thenReturn(Optional.of(inscricao));

        ResultadoCriacaoInscricao resultado = service.criar("participante-1", new CriarInscricaoRequest("evento-1"));

        assertThat(resultado.inscricao().id()).isEqualTo("inscricao-1");
        assertThat(resultado.inscricao().situacao()).isEqualTo(SituacaoInscricao.ATIVA);
        assertThat(resultado.criada()).isFalse();
        verify(inscricaoRepository, never()).save(any());
    }

    @Test
    void postDeInscricaoCanceladaReativaDocumentoExistenteSemMarcarComoCriacao() {
        Evento evento = evento("evento-1", 10, Instant.parse("2026-07-12T15:00:00Z"), Instant.parse("2026-07-12T18:00:00Z"));
        Inscricao inscricao = inscricao("inscricao-1", "evento-1", "participante-1", SituacaoInscricao.ATIVA);
        inscricao.cancelar(AGORA.minusSeconds(120));
        when(eventoRepository.findById("evento-1")).thenReturn(Optional.of(evento));
        when(inscricaoRepository.countByEventoIdAndSituacao("evento-1", SituacaoInscricao.ATIVA)).thenReturn(3L, 4L);
        when(inscricaoRepository.findByEventoIdAndParticipanteId("evento-1", "participante-1")).thenReturn(Optional.of(inscricao));
        when(inscricaoRepository.save(any(Inscricao.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResultadoCriacaoInscricao resultado = service.criar("participante-1", new CriarInscricaoRequest("evento-1"));

        assertThat(resultado.inscricao().id()).isEqualTo("inscricao-1");
        assertThat(resultado.inscricao().situacao()).isEqualTo(SituacaoInscricao.ATIVA);
        assertThat(resultado.inscricao().canceladoEm()).isNull();
        assertThat(resultado.criada()).isFalse();
        verify(inscricaoRepository).save(inscricao);
    }

    @Test
    void inscricaoInexistenteOuDeOutroParticipanteRetornaNaoEncontrado() {
        when(inscricaoRepository.findByIdAndParticipanteId("inscricao-fora", "participante-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelar("participante-1", "inscricao-fora"))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessage("Recurso não encontrado.");
        assertThatThrownBy(() -> service.reativar("participante-1", "inscricao-fora"))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessage("Recurso não encontrado.");
    }

    @Test
    void inscricaoFicaImutavelDepoisDoTerminoDoEvento() {
        Inscricao inscricao = inscricao("inscricao-1", "evento-1", "participante-1", SituacaoInscricao.ATIVA);
        Evento evento = evento("evento-1", 10, Instant.parse("2026-07-10T15:00:00Z"), Instant.parse("2026-07-10T18:00:00Z"));
        when(inscricaoRepository.findByIdAndParticipanteId("inscricao-1", "participante-1")).thenReturn(Optional.of(inscricao));
        when(eventoRepository.findById("evento-1")).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> service.cancelar("participante-1", "inscricao-1"))
            .isInstanceOf(InscricaoNaoPermitidaException.class)
            .hasMessage("Inscrições não podem ser alteradas depois do término do Evento.");
        assertThatThrownBy(() -> service.reativar("participante-1", "inscricao-1"))
            .isInstanceOf(InscricaoNaoPermitidaException.class)
            .hasMessage("Inscrições não podem ser alteradas depois do término do Evento.");

        verify(inscricaoRepository, never()).save(any());
    }

    @Test
    void duplicidadeConcorrenteRetornaDocumentoExistenteSemCriarOutroVinculo() {
        Evento evento = evento("evento-1", 10, Instant.parse("2026-07-12T15:00:00Z"), Instant.parse("2026-07-12T18:00:00Z"));
        Inscricao existente = inscricao("inscricao-existente", "evento-1", "participante-1", SituacaoInscricao.ATIVA);
        when(eventoRepository.findById("evento-1")).thenReturn(Optional.of(evento));
        when(inscricaoRepository.countByEventoIdAndSituacao("evento-1", SituacaoInscricao.ATIVA)).thenReturn(3L);
        when(inscricaoRepository.findByEventoIdAndParticipanteId("evento-1", "participante-1"))
            .thenReturn(Optional.empty(), Optional.of(existente));
        when(inscricaoRepository.save(any(Inscricao.class))).thenThrow(new DuplicateKeyException("duplicado"));

        ResultadoCriacaoInscricao resultado = service.criar("participante-1", new CriarInscricaoRequest("evento-1"));
        InscricaoResponse response = resultado.inscricao();

        assertThat(response.id()).isEqualTo("inscricao-existente");
        assertThat(response.situacao()).isEqualTo(SituacaoInscricao.ATIVA);
        assertThat(resultado.criada()).isFalse();
    }

    private Evento evento(String id, int vagas, Instant iniciaEm, Instant terminaEm) {
        Evento evento = new Evento(
            "organizador-1",
            "Backend Day",
            "Palestras sobre Java e Spring.",
            iniciaEm,
            terminaEm,
            "Auditório 1",
            false,
            "Tecnologia",
            vagas,
            null,
            AGORA.minusSeconds(3600)
        );
        evento.setId(id);
        return evento;
    }

    private Inscricao inscricao(String id, String eventoId, String participanteId, SituacaoInscricao situacao) {
        Inscricao inscricao = new Inscricao(
            eventoId,
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
}
