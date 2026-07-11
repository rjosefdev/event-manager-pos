package com.rjosefdev.eventos_api.inscricoes;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface InscricaoRepository extends MongoRepository<Inscricao, String> {
    long countByEventoIdAndSituacao(String eventoId, SituacaoInscricao situacao);
    List<Inscricao> findByEventoIdOrderByInscritoEmDesc(String eventoId);
    List<Inscricao> findByParticipanteIdOrderByInscritoEmDesc(String participanteId);
    Optional<Inscricao> findByEventoIdAndParticipanteId(String eventoId, String participanteId);
    Optional<Inscricao> findByIdAndParticipanteId(String id, String participanteId);
}
