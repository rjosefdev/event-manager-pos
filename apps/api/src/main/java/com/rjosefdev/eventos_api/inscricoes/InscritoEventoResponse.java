package com.rjosefdev.eventos_api.inscricoes;

import java.time.Instant;

import com.rjosefdev.eventos_api.usuarios.Usuario;

public record InscritoEventoResponse(
    String id,
    String eventoId,
    String participanteId,
    SituacaoInscricao situacao,
    Instant inscritoEm,
    Instant canceladoEm,
    ParticipanteInscritoResponse participante
) {
    public static InscritoEventoResponse de(Inscricao inscricao, Usuario participante) {
        return new InscritoEventoResponse(
            inscricao.getId(),
            inscricao.getEventoId(),
            inscricao.getParticipanteId(),
            inscricao.getSituacao(),
            inscricao.getInscritoEm(),
            inscricao.getCanceladoEm(),
            ParticipanteInscritoResponse.de(participante)
        );
    }
}
