package com.rjosefdev.eventos_api.inscricoes;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.rjosefdev.eventos_api.catalogo.CatalogoEventoResponse;
import com.rjosefdev.eventos_api.eventos.Evento;
import com.rjosefdev.eventos_api.eventos.EventoRepository;
import com.rjosefdev.eventos_api.eventos.RecursoNaoEncontradoException;

@Service
public class InscricaoService {

    private final InscricaoRepository inscricaoRepository;
    private final EventoRepository eventoRepository;
    private final Clock clock;

    public InscricaoService(
        InscricaoRepository inscricaoRepository,
        EventoRepository eventoRepository,
        Clock clock
    ) {
        this.inscricaoRepository = inscricaoRepository;
        this.eventoRepository = eventoRepository;
        this.clock = clock;
    }

    public ResultadoCriacaoInscricao criar(String participanteId, CriarInscricaoRequest request) {
        Instant agora = clock.instant();
        Evento evento = buscarEventoMutavelComVaga(request.eventoId(), agora);

        return inscricaoRepository.findByEventoIdAndParticipanteId(evento.getId(), participanteId)
            .map(inscricao -> responderExistenteOuReativar(inscricao, evento, agora))
            .orElseGet(() -> criarNova(participanteId, evento, agora));
    }

    public List<InscricaoResponse> listar(String participanteId) {
        Instant agora = clock.instant();
        return inscricaoRepository.findByParticipanteIdOrderByInscritoEmDesc(participanteId).stream()
            .map(inscricao -> {
                Evento evento = eventoRepository.findById(inscricao.getEventoId())
                    .orElseThrow(RecursoNaoEncontradoException::new);
                return responder(inscricao, evento, agora);
            })
            .toList();
    }

    public InscricaoResponse cancelar(String participanteId, String inscricaoId) {
        Instant agora = clock.instant();
        Inscricao inscricao = inscricaoRepository.findByIdAndParticipanteId(inscricaoId, participanteId)
            .orElseThrow(RecursoNaoEncontradoException::new);
        Evento evento = buscarEventoMutavel(inscricao.getEventoId(), agora);

        if (inscricao.getSituacao() == SituacaoInscricao.CANCELADA) {
            return responder(inscricao, evento, agora);
        }

        inscricao.cancelar(agora);
        return responder(inscricaoRepository.save(inscricao), evento, agora);
    }

    public InscricaoResponse reativar(String participanteId, String inscricaoId) {
        Instant agora = clock.instant();
        Inscricao inscricao = inscricaoRepository.findByIdAndParticipanteId(inscricaoId, participanteId)
            .orElseThrow(RecursoNaoEncontradoException::new);
        Evento evento = buscarEventoMutavelComVaga(inscricao.getEventoId(), agora);

        if (inscricao.getSituacao() == SituacaoInscricao.ATIVA) {
            return responder(inscricao, evento, agora);
        }

        inscricao.reativar(agora);
        return responder(inscricaoRepository.save(inscricao), evento, agora);
    }

    private ResultadoCriacaoInscricao responderExistenteOuReativar(Inscricao inscricao, Evento evento, Instant agora) {
        if (inscricao.getSituacao() == SituacaoInscricao.CANCELADA) {
            inscricao.reativar(agora);
            return new ResultadoCriacaoInscricao(responder(inscricaoRepository.save(inscricao), evento, agora), false);
        }
        return new ResultadoCriacaoInscricao(responder(inscricao, evento, agora), false);
    }

    private ResultadoCriacaoInscricao criarNova(String participanteId, Evento evento, Instant agora) {
        Inscricao inscricao = new Inscricao(
            evento.getId(),
            participanteId,
            SituacaoInscricao.ATIVA,
            agora,
            null,
            agora,
            agora
        );

        try {
            return new ResultadoCriacaoInscricao(responder(inscricaoRepository.save(inscricao), evento, agora), true);
        } catch (DuplicateKeyException exception) {
            Inscricao existente = inscricaoRepository.findByEventoIdAndParticipanteId(evento.getId(), participanteId)
                .orElseThrow(() -> exception);
            return responderExistenteOuReativar(existente, evento, agora);
        }
    }

    private Evento buscarEventoMutavelComVaga(String eventoId, Instant agora) {
        Evento evento = buscarEventoMutavel(eventoId, agora);
        long ativas = inscricaoRepository.countByEventoIdAndSituacao(evento.getId(), SituacaoInscricao.ATIVA);
        if (ativas >= evento.getVagas()) {
            throw new InscricaoNaoPermitidaException("Não há vagas disponíveis para este Evento.");
        }
        return evento;
    }

    private Evento buscarEventoMutavel(String eventoId, Instant agora) {
        Evento evento = eventoRepository.findById(eventoId)
            .orElseThrow(RecursoNaoEncontradoException::new);
        if (evento.isCancelado()) {
            throw new InscricaoNaoPermitidaException("Inscrições não podem ser alteradas em Evento cancelado.");
        }
        if (!agora.isBefore(evento.getTerminaEm())) {
            throw new InscricaoNaoPermitidaException("Inscrições não podem ser alteradas depois do término do Evento.");
        }
        return evento;
    }

    private InscricaoResponse responder(Inscricao inscricao, Evento evento, Instant agora) {
        long ativas = inscricaoRepository.countByEventoIdAndSituacao(evento.getId(), SituacaoInscricao.ATIVA);
        return InscricaoResponse.de(inscricao, CatalogoEventoResponse.de(evento, agora, ativas));
    }
}
