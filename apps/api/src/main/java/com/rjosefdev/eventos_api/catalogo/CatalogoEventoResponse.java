package com.rjosefdev.eventos_api.catalogo;

import java.time.Instant;

import com.rjosefdev.eventos_api.eventos.Evento;
import com.rjosefdev.eventos_api.eventos.SituacaoTemporalEvento;

public record CatalogoEventoResponse(
    String id,
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
    SituacaoTemporalEvento situacaoTemporal,
    int vagasDisponiveis,
    boolean inscricaoPermitida
) {
    public static CatalogoEventoResponse de(Evento evento, Instant agora, long inscricoesAtivas) {
        SituacaoTemporalEvento situacaoTemporal = calcularSituacaoTemporal(evento, agora);
        int vagasDisponiveis = (int) Math.max(0L, evento.getVagas() - inscricoesAtivas);
        boolean inscricaoPermitida = !evento.isCancelado()
            && situacaoTemporal != SituacaoTemporalEvento.FINALIZADO
            && vagasDisponiveis > 0;

        return new CatalogoEventoResponse(
            evento.getId(),
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
            situacaoTemporal,
            vagasDisponiveis,
            inscricaoPermitida
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
