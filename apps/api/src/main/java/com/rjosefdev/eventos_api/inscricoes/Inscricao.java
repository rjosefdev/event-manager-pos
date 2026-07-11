package com.rjosefdev.eventos_api.inscricoes;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "inscricoes")
public class Inscricao {

    @Id
    private String id;
    private String eventoId;
    private String participanteId;
    private SituacaoInscricao situacao;
    private Instant inscritoEm;
    private Instant canceladoEm;
    private Instant criadoEm;
    private Instant atualizadoEm;

    public Inscricao() {
    }

    public Inscricao(
        String eventoId,
        String participanteId,
        SituacaoInscricao situacao,
        Instant inscritoEm,
        Instant canceladoEm,
        Instant criadoEm,
        Instant atualizadoEm
    ) {
        this.eventoId = eventoId;
        this.participanteId = participanteId;
        this.situacao = situacao;
        this.inscritoEm = inscritoEm;
        this.canceladoEm = canceladoEm;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public String getId() { return id; }
    public String getEventoId() { return eventoId; }
    public String getParticipanteId() { return participanteId; }
    public SituacaoInscricao getSituacao() { return situacao; }
    public Instant getInscritoEm() { return inscritoEm; }
    public Instant getCanceladoEm() { return canceladoEm; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }

    public void setId(String id) { this.id = id; }

    public void cancelar(Instant agora) {
        this.situacao = SituacaoInscricao.CANCELADA;
        this.canceladoEm = agora;
        this.atualizadoEm = agora;
    }

    public void reativar(Instant agora) {
        this.situacao = SituacaoInscricao.ATIVA;
        this.canceladoEm = null;
        this.atualizadoEm = agora;
    }
}
