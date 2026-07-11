package com.rjosefdev.eventos_api.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.rjosefdev.eventos_api.eventos.Evento;
import com.rjosefdev.eventos_api.eventos.EventoRepository;
import com.rjosefdev.eventos_api.eventos.RecursoNaoEncontradoException;
import com.rjosefdev.eventos_api.eventos.SituacaoTemporalEvento;
import com.rjosefdev.eventos_api.inscricoes.InscricaoRepository;
import com.rjosefdev.eventos_api.inscricoes.SituacaoInscricao;

class CatalogoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-07-11T15:00:00Z");

    private final EventoRepository eventoRepository = mock(EventoRepository.class);
    private final InscricaoRepository inscricaoRepository = mock(InscricaoRepository.class);
    private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);
    private final CatalogoService service = new CatalogoService(eventoRepository, inscricaoRepository, clock);

    @Test
    void listaTodosOsEventosComSituacaoVagasDisponiveisEPermissaoDeInscricao() {
        Evento futuro = evento("evento-futuro", "Futuro", 10,
            Instant.parse("2026-07-12T15:00:00Z"), Instant.parse("2026-07-12T18:00:00Z"));
        Evento andamento = evento("evento-andamento", "Em andamento", 5,
            Instant.parse("2026-07-11T14:00:00Z"), Instant.parse("2026-07-11T16:00:00Z"));
        Evento lotado = evento("evento-lotado", "Lotado", 2,
            Instant.parse("2026-07-12T15:00:00Z"), Instant.parse("2026-07-12T18:00:00Z"));
        Evento finalizado = evento("evento-finalizado", "Finalizado", 20,
            Instant.parse("2026-07-10T15:00:00Z"), Instant.parse("2026-07-10T18:00:00Z"));
        Evento cancelado = evento("evento-cancelado", "Cancelado", 30,
            Instant.parse("2026-07-12T15:00:00Z"), Instant.parse("2026-07-12T18:00:00Z"));
        cancelado.cancelar(AGORA);

        when(eventoRepository.findAllByOrderByIniciaEmAsc())
            .thenReturn(List.of(finalizado, andamento, futuro, lotado, cancelado));
        when(inscricaoRepository.countByEventoIdAndSituacao("evento-futuro", SituacaoInscricao.ATIVA)).thenReturn(3L);
        when(inscricaoRepository.countByEventoIdAndSituacao("evento-andamento", SituacaoInscricao.ATIVA)).thenReturn(4L);
        when(inscricaoRepository.countByEventoIdAndSituacao("evento-lotado", SituacaoInscricao.ATIVA)).thenReturn(2L);
        when(inscricaoRepository.countByEventoIdAndSituacao("evento-finalizado", SituacaoInscricao.ATIVA)).thenReturn(6L);
        when(inscricaoRepository.countByEventoIdAndSituacao("evento-cancelado", SituacaoInscricao.ATIVA)).thenReturn(8L);

        List<CatalogoEventoResponse> catalogo = service.listar();

        assertThat(catalogo)
            .extracting(CatalogoEventoResponse::id)
            .containsExactly("evento-finalizado", "evento-andamento", "evento-futuro", "evento-lotado", "evento-cancelado");
        assertThat(catalogo)
            .extracting(CatalogoEventoResponse::situacaoTemporal)
            .containsExactly(
                SituacaoTemporalEvento.FINALIZADO,
                SituacaoTemporalEvento.EM_ANDAMENTO,
                SituacaoTemporalEvento.FUTURO,
                SituacaoTemporalEvento.FUTURO,
                SituacaoTemporalEvento.FUTURO
            );
        assertThat(catalogo)
            .extracting(CatalogoEventoResponse::vagasDisponiveis)
            .containsExactly(14, 1, 7, 0, 22);
        assertThat(catalogo)
            .extracting(CatalogoEventoResponse::inscricaoPermitida)
            .containsExactly(false, true, true, false, false);
    }

    @Test
    void listaComBuscaCategoriaEOrdenacaoDecrescente() {
        Evento backend = evento("evento-backend", "Backend Day", 10,
            Instant.parse("2026-07-12T15:00:00Z"), Instant.parse("2026-07-12T18:00:00Z"));
        Evento spring = evento("evento-spring", "Spring Night", 10,
            Instant.parse("2026-07-13T15:00:00Z"), Instant.parse("2026-07-13T18:00:00Z"));
        Evento frontend = evento("evento-frontend", "Frontend Summit", 10,
            Instant.parse("2026-07-14T15:00:00Z"), Instant.parse("2026-07-14T18:00:00Z"));
        frontend.editar(
            frontend.getTitulo(),
            frontend.getDescricao(),
            frontend.getIniciaEm(),
            frontend.getTerminaEm(),
            frontend.getLocal(),
            frontend.isOnline(),
            "Frontend",
            frontend.getVagas(),
            frontend.getImagemUrl(),
            AGORA
        );

        when(eventoRepository.findAllByOrderByIniciaEmAsc())
            .thenReturn(List.of(backend, spring, frontend));

        List<CatalogoEventoResponse> catalogo = service.listar(
            new CatalogoFiltro("evento", "Tecnologia", "desc")
        );

        assertThat(catalogo)
            .extracting(CatalogoEventoResponse::id)
            .containsExactly("evento-spring", "evento-backend");
    }

    @Test
    void detalhesRetornamEventoDoCatalogoComVagasCalculadas() {
        Evento evento = evento("evento-1", "Backend Day", 80,
            Instant.parse("2026-07-12T15:00:00Z"), Instant.parse("2026-07-12T18:00:00Z"));
        when(eventoRepository.findById("evento-1")).thenReturn(Optional.of(evento));
        when(inscricaoRepository.countByEventoIdAndSituacao("evento-1", SituacaoInscricao.ATIVA)).thenReturn(12L);

        CatalogoEventoResponse detalhes = service.buscarDetalhes("evento-1");

        assertThat(detalhes.id()).isEqualTo("evento-1");
        assertThat(detalhes.titulo()).isEqualTo("Backend Day");
        assertThat(detalhes.vagasDisponiveis()).isEqualTo(68);
        assertThat(detalhes.inscricaoPermitida()).isTrue();
        verify(eventoRepository).findById("evento-1");
    }

    @Test
    void eventoInexistenteRetornaNaoEncontradoSemContarInscricoes() {
        when(eventoRepository.findById("evento-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarDetalhes("evento-inexistente"))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessage("Recurso não encontrado.");
    }

    private Evento evento(String id, String titulo, int vagas, Instant iniciaEm, Instant terminaEm) {
        Evento evento = new Evento(
            "organizador-1",
            titulo,
            "Descrição do evento.",
            iniciaEm,
            terminaEm,
            "Auditório",
            false,
            "Tecnologia",
            vagas,
            null,
            AGORA
        );
        evento.setId(id);
        return evento;
    }
}
