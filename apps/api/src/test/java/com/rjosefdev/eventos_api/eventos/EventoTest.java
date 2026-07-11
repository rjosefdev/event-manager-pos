package com.rjosefdev.eventos_api.eventos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class EventoTest {

    private static final Instant CRIADO_EM = Instant.parse("2026-07-11T15:00:00Z");
    private static final Instant ATUALIZADO_EM = Instant.parse("2026-07-11T16:00:00Z");

    @Test
    void anexaERemoveMetadadosDeImagemDeArquivoMantendoUrlExterna() {
        Evento evento = new Evento(
            "organizador-1",
            "Backend Day",
            "Palestras sobre Java e Spring.",
            Instant.parse("2026-07-12T15:00:00Z"),
            Instant.parse("2026-07-12T18:00:00Z"),
            "Auditório 1",
            false,
            "Tecnologia",
            80,
            "https://exemplo.com/banner.png",
            CRIADO_EM
        );
        evento.setId("evento-1");

        evento.anexarImagemArquivo("arquivo-1", "banner.png", "image/png", 84512L, ATUALIZADO_EM);

        assertThat(evento.getImagemArquivoId()).isEqualTo("arquivo-1");
        assertThat(evento.getImagemArquivoNome()).isEqualTo("banner.png");
        assertThat(evento.getImagemContentType()).isEqualTo("image/png");
        assertThat(evento.getImagemTamanhoBytes()).isEqualTo(84512L);
        assertThat(evento.getImagemUrl()).isEqualTo("https://exemplo.com/banner.png");
        assertThat(evento.getImagemUrlEfetiva()).isEqualTo("/catalogo/eventos/evento-1/imagem");
        assertThat(evento.possuiImagemArquivo()).isTrue();
        assertThat(evento.getAtualizadoEm()).isEqualTo(ATUALIZADO_EM);

        evento.removerImagemArquivo(ATUALIZADO_EM.plusSeconds(60));

        assertThat(evento.getImagemArquivoId()).isNull();
        assertThat(evento.getImagemArquivoNome()).isNull();
        assertThat(evento.getImagemContentType()).isNull();
        assertThat(evento.getImagemTamanhoBytes()).isNull();
        assertThat(evento.getImagemUrl()).isEqualTo("https://exemplo.com/banner.png");
        assertThat(evento.getImagemUrlEfetiva()).isEqualTo("https://exemplo.com/banner.png");
        assertThat(evento.possuiImagemArquivo()).isFalse();
        assertThat(evento.getAtualizadoEm()).isEqualTo(ATUALIZADO_EM.plusSeconds(60));
    }
}
