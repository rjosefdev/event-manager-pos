package com.rjosefdev.eventos_api.inscricoes;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;

import com.rjosefdev.eventos_api.eventos.Evento;
import com.rjosefdev.eventos_api.eventos.EventoRepository;
import com.rjosefdev.eventos_api.eventos.RecursoNaoEncontradoException;
import com.rjosefdev.eventos_api.usuarios.Usuario;
import com.rjosefdev.eventos_api.usuarios.UsuarioRepository;

@Service
public class InscritosEventoService {

    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;
    private final UsuarioRepository usuarioRepository;

    public InscritosEventoService(
            EventoRepository eventoRepository,
            InscricaoRepository inscricaoRepository,
            UsuarioRepository usuarioRepository) {
        this.eventoRepository = eventoRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<InscritoEventoResponse> listarDoEvento(String organizadorId, String eventoId) {
        Evento evento = eventoRepository.findByIdAndOrganizadorId(eventoId, organizadorId)
                .orElseThrow(RecursoNaoEncontradoException::new);
        List<Inscricao> inscricoes = inscricaoRepository.findByEventoIdOrderByInscritoEmDesc(evento.getId());
        List<String> participantesIds = inscricoes.stream()
                .map(inscricao -> inscricao.getParticipanteId())
                .distinct()
                .toList();

        Map<String, Usuario> participantes = StreamSupport.stream(
                usuarioRepository.findAllById(participantesIds).spliterator(),
                false)
                .collect(Collectors.toMap(usuario -> usuario.getId(), Function.identity()));

        return inscricoes.stream()
                .map(inscricao -> InscritoEventoResponse.de(inscricao,
                        participanteDaInscricao(participantes, inscricao)))
                .toList();
    }

    private Usuario participanteDaInscricao(Map<String, Usuario> participantes, Inscricao inscricao) {
        Usuario participante = participantes.get(inscricao.getParticipanteId());
        if (participante == null) {
            throw new RecursoNaoEncontradoException();
        }
        return participante;
    }
}
