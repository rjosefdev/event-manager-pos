package com.rjosefdev.eventos_api.eventos;

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

class EventoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-07-11T15:00:00Z");

    private final EventoRepository repository = mock(EventoRepository.class);
    private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);
    private final EventoService service = new EventoService(repository, clock);

    @Test
    void criaEventoDerivandoOrganizadorIdDoSubDoJwt() {
        when(repository.save(any(Evento.class))).thenAnswer(invocation -> {
            Evento evento = invocation.getArgument(0);
            evento.setId("evento-1");
            return evento;
        });

        EventoResponse response = service.criar("organizador-1", requestPadrao());

        ArgumentCaptor<Evento> eventoCaptor = ArgumentCaptor.forClass(Evento.class);
        verify(repository).save(eventoCaptor.capture());
        Evento persistido = eventoCaptor.getValue();

        assertThat(persistido.getOrganizadorId()).isEqualTo("organizador-1");
        assertThat(persistido.getTitulo()).isEqualTo("Backend Day");
        assertThat(persistido.getDescricao()).isEqualTo("Palestras sobre Java e Spring.");
        assertThat(persistido.getLocal()).isEqualTo("Auditório 1");
        assertThat(persistido.getCategoria()).isEqualTo("Tecnologia");
        assertThat(persistido.getVagas()).isEqualTo(80);
        assertThat(persistido.getImagemUrl()).isNull();
        assertThat(persistido.isCancelado()).isFalse();
        assertThat(persistido.getCriadoEm()).isEqualTo(AGORA);
        assertThat(response.organizadorId()).isEqualTo("organizador-1");
        assertThat(response.situacaoTemporal()).isEqualTo(SituacaoTemporalEvento.FUTURO);
    }

    @Test
    void listaVaziaDoOrganizadorAutenticado() {
        when(repository.findByOrganizadorIdOrderByIniciaEmAsc("organizador-1")).thenReturn(List.of());

        List<EventoResponse> eventos = service.listarDoOrganizador("organizador-1");

        assertThat(eventos).isEmpty();
        verify(repository).findByOrganizadorIdOrderByIniciaEmAsc("organizador-1");
    }

    @Test
    void listaSomenteEventosRetornadosPeloFiltroDeOrganizador() {
        Evento futuro = evento("evento-futuro", "organizador-1", "Futuro",
            Instant.parse("2026-07-12T15:00:00Z"), Instant.parse("2026-07-12T18:00:00Z"));
        Evento andamento = evento("evento-andamento", "organizador-1", "Em andamento",
            Instant.parse("2026-07-11T14:00:00Z"), Instant.parse("2026-07-11T16:00:00Z"));
        Evento finalizado = evento("evento-finalizado", "organizador-1", "Finalizado",
            Instant.parse("2026-07-10T14:00:00Z"), Instant.parse("2026-07-10T16:00:00Z"));
        when(repository.findByOrganizadorIdOrderByIniciaEmAsc("organizador-1"))
            .thenReturn(List.of(finalizado, andamento, futuro));

        List<EventoResponse> eventos = service.listarDoOrganizador("organizador-1");

        assertThat(eventos)
            .extracting(EventoResponse::id)
            .containsExactly("evento-finalizado", "evento-andamento", "evento-futuro");
        assertThat(eventos)
            .extracting(EventoResponse::organizadorId)
            .containsOnly("organizador-1");
        assertThat(eventos)
            .extracting(EventoResponse::situacaoTemporal)
            .containsExactly(
                SituacaoTemporalEvento.FINALIZADO,
                SituacaoTemporalEvento.EM_ANDAMENTO,
                SituacaoTemporalEvento.FUTURO
            );
    }

    @Test
    void buscaEventoProprioSemExigirQueEstejaEditavel() {
        Evento finalizado = evento("evento-1", "organizador-1", "Finalizado",
            Instant.parse("2026-07-10T14:00:00Z"), Instant.parse("2026-07-10T16:00:00Z"));
        when(repository.findByIdAndOrganizadorId("evento-1", "organizador-1")).thenReturn(Optional.of(finalizado));

        EventoResponse response = service.buscarDoOrganizador("organizador-1", "evento-1");

        assertThat(response.id()).isEqualTo("evento-1");
        assertThat(response.organizadorId()).isEqualTo("organizador-1");
        assertThat(response.situacaoTemporal()).isEqualTo(SituacaoTemporalEvento.FINALIZADO);
    }

    @Test
    void rejeitaPeriodoSemTerminoPosteriorAoInicio() {
        CriarEventoRequest request = new CriarEventoRequest(
            "Backend Day",
            "Palestras sobre Java e Spring.",
            Instant.parse("2026-07-12T18:00:00Z"),
            Instant.parse("2026-07-12T18:00:00Z"),
            "Auditório 1",
            false,
            "Tecnologia",
            80,
            null
        );

        assertThatThrownBy(() -> service.criar("organizador-1", request))
            .isInstanceOf(PeriodoEventoInvalidoException.class)
            .hasMessage("A data de término deve ser posterior à data de início.");
    }

    @Test
    void atualizaEventoProprioAindaNaoFinalizado() {
        Evento evento = evento("evento-1", "organizador-1", "Backend Day",
            Instant.parse("2026-07-12T15:00:00Z"), Instant.parse("2026-07-12T18:00:00Z"));
        when(repository.findByIdAndOrganizadorId("evento-1", "organizador-1")).thenReturn(Optional.of(evento));
        when(repository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventoResponse response = service.atualizar("organizador-1", "evento-1", atualizarRequestPadrao());

        ArgumentCaptor<Evento> eventoCaptor = ArgumentCaptor.forClass(Evento.class);
        verify(repository).save(eventoCaptor.capture());
        Evento persistido = eventoCaptor.getValue();

        assertThat(persistido.getTitulo()).isEqualTo("Frontend Summit");
        assertThat(persistido.getDescricao()).isEqualTo("Palestras sobre React e Next.");
        assertThat(persistido.getIniciaEm()).isEqualTo(Instant.parse("2026-07-13T15:00:00Z"));
        assertThat(persistido.getTerminaEm()).isEqualTo(Instant.parse("2026-07-13T18:00:00Z"));
        assertThat(persistido.getLocal()).isEqualTo("Sala 2");
        assertThat(persistido.isOnline()).isTrue();
        assertThat(persistido.getCategoria()).isEqualTo("Frontend");
        assertThat(persistido.getVagas()).isEqualTo(50);
        assertThat(persistido.getImagemUrl()).isEqualTo("https://exemplo.com/frontend.png");
        assertThat(persistido.getAtualizadoEm()).isEqualTo(AGORA);
        assertThat(response.titulo()).isEqualTo("Frontend Summit");
        assertThat(response.situacaoTemporal()).isEqualTo(SituacaoTemporalEvento.FUTURO);
    }

    @Test
    void cancelarEventoProprioDefineCanceladoSemRemoverDocumento() {
        Evento evento = evento("evento-1", "organizador-1", "Backend Day",
            Instant.parse("2026-07-12T15:00:00Z"), Instant.parse("2026-07-12T18:00:00Z"));
        when(repository.findByIdAndOrganizadorId("evento-1", "organizador-1")).thenReturn(Optional.of(evento));
        when(repository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventoResponse response = service.cancelar("organizador-1", "evento-1");

        ArgumentCaptor<Evento> eventoCaptor = ArgumentCaptor.forClass(Evento.class);
        verify(repository).save(eventoCaptor.capture());
        verify(repository, never()).delete(any());
        assertThat(eventoCaptor.getValue().isCancelado()).isTrue();
        assertThat(eventoCaptor.getValue().getAtualizadoEm()).isEqualTo(AGORA);
        assertThat(response.cancelado()).isTrue();
    }

    @Test
    void outroOrganizadorRecebeMesmoNaoEncontradoDeEventoInexistente() {
        when(repository.findByIdAndOrganizadorId("evento-1", "organizador-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar("organizador-2", "evento-1", atualizarRequestPadrao()))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessage("Recurso não encontrado.");

        assertThatThrownBy(() -> service.cancelar("organizador-2", "evento-1"))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessage("Recurso não encontrado.");
    }

    @Test
    void rejeitaEdicaoECancelamentoDeEventoFinalizado() {
        Evento evento = evento("evento-1", "organizador-1", "Finalizado",
            Instant.parse("2026-07-10T15:00:00Z"), Instant.parse("2026-07-10T18:00:00Z"));
        when(repository.findByIdAndOrganizadorId("evento-1", "organizador-1")).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> service.atualizar("organizador-1", "evento-1", atualizarRequestPadrao()))
            .isInstanceOf(EventoFinalizadoException.class)
            .hasMessage("Eventos finalizados não podem ser alterados ou cancelados.");

        assertThatThrownBy(() -> service.cancelar("organizador-1", "evento-1"))
            .isInstanceOf(EventoFinalizadoException.class)
            .hasMessage("Eventos finalizados não podem ser alterados ou cancelados.");

        verify(repository, never()).save(any());
    }

    private CriarEventoRequest requestPadrao() {
        return new CriarEventoRequest(
            "  Backend Day  ",
            "  Palestras sobre Java e Spring.  ",
            Instant.parse("2026-07-12T15:00:00Z"),
            Instant.parse("2026-07-12T18:00:00Z"),
            "  Auditório 1  ",
            false,
            "  Tecnologia  ",
            80,
            "   "
        );
    }

    private AtualizarEventoRequest atualizarRequestPadrao() {
        return new AtualizarEventoRequest(
            "  Frontend Summit  ",
            "  Palestras sobre React e Next.  ",
            Instant.parse("2026-07-13T15:00:00Z"),
            Instant.parse("2026-07-13T18:00:00Z"),
            "  Sala 2  ",
            true,
            "  Frontend  ",
            50,
            "  https://exemplo.com/frontend.png  "
        );
    }

    private Evento evento(String id, String organizadorId, String titulo, Instant iniciaEm, Instant terminaEm) {
        Evento evento = new Evento(
            organizadorId,
            titulo,
            "Descrição",
            iniciaEm,
            terminaEm,
            "Auditório",
            false,
            "Tecnologia",
            80,
            null,
            AGORA
        );
        evento.setId(id);
        return evento;
    }
}
