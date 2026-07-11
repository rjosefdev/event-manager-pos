package com.rjosefdev.eventos_api.catalogo;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.rjosefdev.eventos_api.eventos.Evento;
import com.rjosefdev.eventos_api.eventos.EventoRepository;
import com.rjosefdev.eventos_api.eventos.RecursoNaoEncontradoException;
import com.rjosefdev.eventos_api.inscricoes.InscricaoRepository;
import com.rjosefdev.eventos_api.inscricoes.SituacaoInscricao;

@Service
public class CatalogoService {

    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;
    private final Clock clock;

    public CatalogoService(
        EventoRepository eventoRepository,
        InscricaoRepository inscricaoRepository,
        Clock clock
    ) {
        this.eventoRepository = eventoRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.clock = clock;
    }

    public List<CatalogoEventoResponse> listar() {
        return listar(CatalogoFiltro.semFiltros());
    }

    public List<CatalogoEventoResponse> listar(CatalogoFiltro filtro) {
        Instant agora = clock.instant();
        Comparator<Evento> ordenacao = Comparator.comparing(evento -> evento.getIniciaEm());
        if (filtro.ordemDecrescente()) {
            ordenacao = ordenacao.reversed();
        }

        return eventoRepository.findAllByOrderByIniciaEmAsc().stream()
            .filter(filtro::corresponde)
            .sorted(ordenacao)
            .map(evento -> responder(evento, agora))
            .toList();
    }

    public CatalogoEventoResponse buscarDetalhes(String eventoId) {
        Instant agora = clock.instant();
        Evento evento = eventoRepository.findById(eventoId)
            .orElseThrow(RecursoNaoEncontradoException::new);
        return responder(evento, agora);
    }

    private CatalogoEventoResponse responder(Evento evento, Instant agora) {
        long inscricoesAtivas = inscricaoRepository.countByEventoIdAndSituacao(
            evento.getId(),
            SituacaoInscricao.ATIVA
        );
        return CatalogoEventoResponse.de(evento, agora, inscricoesAtivas);
    }
}
