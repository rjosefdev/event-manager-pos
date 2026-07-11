package com.rjosefdev.eventos_api.eventos;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final Clock clock;

    public EventoService(EventoRepository eventoRepository, Clock clock) {
        this.eventoRepository = eventoRepository;
        this.clock = clock;
    }

    public EventoResponse criar(String organizadorId, CriarEventoRequest request) {
        validarPeriodo(request.iniciaEm(), request.terminaEm());
        Instant agora = clock.instant();
        Evento evento = new Evento(
            organizadorId,
            request.titulo().trim(),
            request.descricao().trim(),
            request.iniciaEm(),
            request.terminaEm(),
            request.local().trim(),
            request.online(),
            request.categoria().trim(),
            request.vagas(),
            normalizarOpcional(request.imagemUrl()),
            agora
        );

        return EventoResponse.de(eventoRepository.save(evento), agora);
    }

    public List<EventoResponse> listarDoOrganizador(String organizadorId) {
        Instant agora = clock.instant();
        return eventoRepository.findByOrganizadorIdOrderByIniciaEmAsc(organizadorId).stream()
            .map(evento -> EventoResponse.de(evento, agora))
            .toList();
    }

    public EventoResponse buscarDoOrganizador(String organizadorId, String eventoId) {
        Instant agora = clock.instant();
        Evento evento = eventoRepository.findByIdAndOrganizadorId(eventoId, organizadorId)
            .orElseThrow(RecursoNaoEncontradoException::new);
        return EventoResponse.de(evento, agora);
    }

    public EventoResponse atualizar(String organizadorId, String eventoId, AtualizarEventoRequest request) {
        validarPeriodo(request.iniciaEm(), request.terminaEm());
        Instant agora = clock.instant();
        Evento evento = buscarEventoProprioEditavel(organizadorId, eventoId, agora);
        evento.editar(
            request.titulo().trim(),
            request.descricao().trim(),
            request.iniciaEm(),
            request.terminaEm(),
            request.local().trim(),
            request.online(),
            request.categoria().trim(),
            request.vagas(),
            normalizarOpcional(request.imagemUrl()),
            agora
        );

        return EventoResponse.de(eventoRepository.save(evento), agora);
    }

    public EventoResponse cancelar(String organizadorId, String eventoId) {
        Instant agora = clock.instant();
        Evento evento = buscarEventoProprioEditavel(organizadorId, eventoId, agora);
        evento.cancelar(agora);

        return EventoResponse.de(eventoRepository.save(evento), agora);
    }

    private Evento buscarEventoProprioEditavel(String organizadorId, String eventoId, Instant agora) {
        Evento evento = eventoRepository.findByIdAndOrganizadorId(eventoId, organizadorId)
            .orElseThrow(RecursoNaoEncontradoException::new);
        if (!agora.isBefore(evento.getTerminaEm())) {
            throw new EventoFinalizadoException();
        }
        return evento;
    }

    private void validarPeriodo(Instant iniciaEm, Instant terminaEm) {
        if (iniciaEm == null || terminaEm == null) {
            return;
        }
        if (!terminaEm.isAfter(iniciaEm)) {
            throw new PeriodoEventoInvalidoException();
        }
    }

    private String normalizarOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
