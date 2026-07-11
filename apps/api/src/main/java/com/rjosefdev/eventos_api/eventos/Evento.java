package com.rjosefdev.eventos_api.eventos;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "eventos")
public class Evento {

    @Id
    private String id;
    private String organizadorId;
    private String titulo;
    private String descricao;
    private Instant iniciaEm;
    private Instant terminaEm;
    private String local;
    private boolean online;
    private String categoria;
    private int vagas;
    private String imagemUrl;
    private String imagemArquivoId;
    private String imagemArquivoNome;
    private String imagemContentType;
    private Long imagemTamanhoBytes;
    private boolean cancelado;
    private Instant criadoEm;
    private Instant atualizadoEm;

    public Evento() {
    }

    public Evento(
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
        Instant criadoEm
    ) {
        this.organizadorId = organizadorId;
        this.titulo = titulo;
        this.descricao = descricao;
        this.iniciaEm = iniciaEm;
        this.terminaEm = terminaEm;
        this.local = local;
        this.online = online;
        this.categoria = categoria;
        this.vagas = vagas;
        this.imagemUrl = imagemUrl;
        this.cancelado = false;
        this.criadoEm = criadoEm;
        this.atualizadoEm = criadoEm;
    }

    public String getId() { return id; }
    public String getOrganizadorId() { return organizadorId; }
    public String getTitulo() { return titulo; }
    public String getDescricao() { return descricao; }
    public Instant getIniciaEm() { return iniciaEm; }
    public Instant getTerminaEm() { return terminaEm; }
    public String getLocal() { return local; }
    public boolean isOnline() { return online; }
    public String getCategoria() { return categoria; }
    public int getVagas() { return vagas; }
    public String getImagemUrl() { return imagemUrl; }
    public String getImagemArquivoId() { return imagemArquivoId; }
    public String getImagemArquivoNome() { return imagemArquivoNome; }
    public String getImagemContentType() { return imagemContentType; }
    public Long getImagemTamanhoBytes() { return imagemTamanhoBytes; }
    public boolean isCancelado() { return cancelado; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }

    public void setId(String id) { this.id = id; }

    public boolean possuiImagemArquivo() {
        return imagemArquivoId != null && !imagemArquivoId.isBlank();
    }

    public String getImagemUrlEfetiva() {
        if (possuiImagemArquivo()) {
            return "/catalogo/eventos/" + id + "/imagem";
        }
        return imagemUrl;
    }

    public void editar(
        String titulo,
        String descricao,
        Instant iniciaEm,
        Instant terminaEm,
        String local,
        boolean online,
        String categoria,
        int vagas,
        String imagemUrl,
        Instant atualizadoEm
    ) {
        this.titulo = titulo;
        this.descricao = descricao;
        this.iniciaEm = iniciaEm;
        this.terminaEm = terminaEm;
        this.local = local;
        this.online = online;
        this.categoria = categoria;
        this.vagas = vagas;
        this.imagemUrl = imagemUrl;
        this.atualizadoEm = atualizadoEm;
    }

    public void cancelar(Instant atualizadoEm) {
        this.cancelado = true;
        this.atualizadoEm = atualizadoEm;
    }

    public void anexarImagemArquivo(
        String imagemArquivoId,
        String imagemArquivoNome,
        String imagemContentType,
        Long imagemTamanhoBytes,
        Instant atualizadoEm
    ) {
        this.imagemArquivoId = imagemArquivoId;
        this.imagemArquivoNome = imagemArquivoNome;
        this.imagemContentType = imagemContentType;
        this.imagemTamanhoBytes = imagemTamanhoBytes;
        this.atualizadoEm = atualizadoEm;
    }

    public void removerImagemArquivo(Instant atualizadoEm) {
        this.imagemArquivoId = null;
        this.imagemArquivoNome = null;
        this.imagemContentType = null;
        this.imagemTamanhoBytes = null;
        this.atualizadoEm = atualizadoEm;
    }
}
