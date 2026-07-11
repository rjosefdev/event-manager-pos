package com.rjosefdev.eventos_api.inscricoes;

import java.time.Instant;

import com.rjosefdev.eventos_api.catalogo.CatalogoEventoResponse;

public record InscricaoResponse(
    String id,
    String eventoId,
    String participanteId,
    SituacaoInscricao situacao,
    Instant inscritoEm,
    Instant canceladoEm,
    CatalogoEventoResponse evento
) {
    public static InscricaoResponse de(Inscricao inscricao, CatalogoEventoResponse evento) {
        return new InscricaoResponse(
            inscricao.getId(),
            inscricao.getEventoId(),
            inscricao.getParticipanteId(),
            inscricao.getSituacao(),
            inscricao.getInscritoEm(),
            inscricao.getCanceladoEm(),
            evento
        );
    }
}
