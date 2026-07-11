package com.rjosefdev.eventos_api.eventos;

import java.time.Instant;

public record EventoResponse(
    String id,
    String organizadorId,
    String titulo,
    String descricao,
    Instant iniciaEm,
    Instant terminaEm,
    String local,
    boolean online,
    String categoria,
    int vagas,
    String imagemUrl,
    boolean cancelado,
    SituacaoTemporalEvento situacaoTemporal
) {
    public static EventoResponse de(Evento evento, Instant agora) {
        return new EventoResponse(
            evento.getId(),
            evento.getOrganizadorId(),
            evento.getTitulo(),
            evento.getDescricao(),
            evento.getIniciaEm(),
            evento.getTerminaEm(),
            evento.getLocal(),
            evento.isOnline(),
            evento.getCategoria(),
            evento.getVagas(),
            evento.getImagemUrl(),
            evento.isCancelado(),
            calcularSituacaoTemporal(evento, agora)
        );
    }

    private static SituacaoTemporalEvento calcularSituacaoTemporal(Evento evento, Instant agora) {
        if (agora.isBefore(evento.getIniciaEm())) {
            return SituacaoTemporalEvento.FUTURO;
        }
        if (agora.isBefore(evento.getTerminaEm())) {
            return SituacaoTemporalEvento.EM_ANDAMENTO;
        }
        return SituacaoTemporalEvento.FINALIZADO;
    }
}
