package com.rjosefdev.eventos_api.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;

import com.mongodb.client.gridfs.model.GridFSFile;

class EventoImagemServiceTest {

    private static final Instant AGORA = Instant.parse("2026-07-11T15:00:00Z");
    private static final String ARQUIVO_NOVO_ID = "66b000000000000000000301";
    private static final String ARQUIVO_ANTIGO_ID = "66b000000000000000000201";

    private final EventoRepository eventoRepository = mock(EventoRepository.class);
    private final GridFsTemplate gridFsTemplate = mock(GridFsTemplate.class);
    private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);
    private final EventoImagemService service = new EventoImagemService(eventoRepository, gridFsTemplate, clock);

    @Test
    void salvaArquivoNoGridFsEAtualizaMetadadosDoEvento() {
        Evento evento = eventoFuturo();
        MockMultipartFile arquivo = imagemPng("banner.png", "conteudo".getBytes());
        when(eventoRepository.findByIdAndOrganizadorId("evento-1", "organizador-1")).thenReturn(Optional.of(evento));
        when(gridFsTemplate.store(any(InputStream.class), eq("banner.png"), eq("image/png"), any(Document.class)))
            .thenReturn(new ObjectId(ARQUIVO_NOVO_ID));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventoResponse response = service.anexar("organizador-1", "evento-1", arquivo);

        ArgumentCaptor<Document> metadataCaptor = ArgumentCaptor.forClass(Document.class);
        verify(gridFsTemplate).store(any(InputStream.class), eq("banner.png"), eq("image/png"), metadataCaptor.capture());
        Document metadata = metadataCaptor.getValue();
        assertThat(metadata.getString("eventoId")).isEqualTo("evento-1");
        assertThat(metadata.getString("organizadorId")).isEqualTo("organizador-1");
        assertThat(metadata.getString("contentType")).isEqualTo("image/png");
        assertThat(metadata.getString("originalFilename")).isEqualTo("banner.png");
        assertThat(metadata.getLong("tamanhoBytes")).isEqualTo(8L);

        ArgumentCaptor<Evento> eventoCaptor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(eventoCaptor.capture());
        Evento persistido = eventoCaptor.getValue();
        assertThat(persistido.getImagemArquivoId()).isEqualTo(ARQUIVO_NOVO_ID);
        assertThat(persistido.getImagemArquivoNome()).isEqualTo("banner.png");
        assertThat(persistido.getImagemContentType()).isEqualTo("image/png");
        assertThat(persistido.getImagemTamanhoBytes()).isEqualTo(8L);
        assertThat(persistido.getAtualizadoEm()).isEqualTo(AGORA);
        assertThat(response.imagemUrl()).isEqualTo("/catalogo/eventos/evento-1/imagem");
        assertThat(response.possuiImagemArquivo()).isTrue();
        verify(gridFsTemplate, never()).delete(any(Query.class));
    }

    @Test
    void rejeitaArquivoVazio() {
        MockMultipartFile arquivo = imagemPng("banner.png", new byte[0]);

        assertThatThrownBy(() -> service.anexar("organizador-1", "evento-1", arquivo))
            .isInstanceOf(EventoImagemInvalidaException.class)
            .hasMessage("Informe um arquivo de imagem.");

        verifyNoInteractions(eventoRepository, gridFsTemplate);
    }

    @Test
    void rejeitaContentTypeNaoPermitido() {
        MockMultipartFile arquivo = new MockMultipartFile(
            "arquivo",
            "banner.txt",
            "text/plain",
            "texto".getBytes()
        );

        assertThatThrownBy(() -> service.anexar("organizador-1", "evento-1", arquivo))
            .isInstanceOf(EventoImagemInvalidaException.class)
            .hasMessage("Tipo de imagem não permitido.");

        verifyNoInteractions(eventoRepository, gridFsTemplate);
    }

    @Test
    void rejeitaArquivoMaiorQueCincoMb() {
        MockMultipartFile arquivo = imagemPng("banner.png", new byte[(5 * 1024 * 1024) + 1]);

        assertThatThrownBy(() -> service.anexar("organizador-1", "evento-1", arquivo))
            .isInstanceOf(EventoImagemInvalidaException.class)
            .hasMessage("A imagem deve ter no máximo 5 MB.");

        verifyNoInteractions(eventoRepository, gridFsTemplate);
    }

    @Test
    void substituiImagemRemovendoArquivoAntigoDepoisDeSalvarEvento() {
        Evento evento = eventoFuturo();
        evento.anexarImagemArquivo(ARQUIVO_ANTIGO_ID, "antigo.png", "image/png", 10L, AGORA.minusSeconds(60));
        MockMultipartFile arquivo = imagemPng("novo.png", "nova".getBytes());
        when(eventoRepository.findByIdAndOrganizadorId("evento-1", "organizador-1")).thenReturn(Optional.of(evento));
        when(gridFsTemplate.store(any(InputStream.class), eq("novo.png"), eq("image/png"), any(Document.class)))
            .thenReturn(new ObjectId(ARQUIVO_NOVO_ID));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.anexar("organizador-1", "evento-1", arquivo);

        InOrder ordem = inOrder(gridFsTemplate, eventoRepository);
        ordem.verify(gridFsTemplate).store(any(InputStream.class), eq("novo.png"), eq("image/png"), any(Document.class));
        ordem.verify(eventoRepository).save(any(Evento.class));
        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        ordem.verify(gridFsTemplate).delete(queryCaptor.capture());

        assertThat(queryCaptor.getValue().getQueryObject().get("_id"))
            .isEqualTo(new ObjectId(ARQUIVO_ANTIGO_ID));
    }

    @Test
    void removeArquivoNovoSeFalharAoSalvarEvento() {
        Evento evento = eventoFuturo();
        MockMultipartFile arquivo = imagemPng("banner.png", "conteudo".getBytes());
        RuntimeException falha = new RuntimeException("mongo indisponivel");
        when(eventoRepository.findByIdAndOrganizadorId("evento-1", "organizador-1")).thenReturn(Optional.of(evento));
        when(gridFsTemplate.store(any(InputStream.class), eq("banner.png"), eq("image/png"), any(Document.class)))
            .thenReturn(new ObjectId(ARQUIVO_NOVO_ID));
        when(eventoRepository.save(any(Evento.class))).thenThrow(falha);

        assertThatThrownBy(() -> service.anexar("organizador-1", "evento-1", arquivo))
            .isSameAs(falha);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(gridFsTemplate).delete(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getQueryObject().get("_id"))
            .isEqualTo(new ObjectId(ARQUIVO_NOVO_ID));
    }

    @Test
    void rejeitaEventoDeOutroOrganizadorSemGravarArquivo() {
        MockMultipartFile arquivo = imagemPng("banner.png", "conteudo".getBytes());
        when(eventoRepository.findByIdAndOrganizadorId("evento-1", "organizador-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.anexar("organizador-2", "evento-1", arquivo))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessage("Recurso não encontrado.");

        verify(gridFsTemplate, never()).store(any(InputStream.class), any(), any(), any());
        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void rejeitaEventoFinalizadoSemGravarArquivo() {
        Evento evento = eventoFinalizado();
        MockMultipartFile arquivo = imagemPng("banner.png", "conteudo".getBytes());
        when(eventoRepository.findByIdAndOrganizadorId("evento-1", "organizador-1")).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> service.anexar("organizador-1", "evento-1", arquivo))
            .isInstanceOf(EventoFinalizadoException.class)
            .hasMessage("Eventos finalizados não podem ser alterados ou cancelados.");

        verify(gridFsTemplate, never()).store(any(InputStream.class), any(), any(), any());
        verify(eventoRepository, never()).save(any(Evento.class));
    }

    @Test
    void removeImagemDeArquivoMantendoUrlExterna() {
        Evento evento = eventoFuturo();
        evento.anexarImagemArquivo(ARQUIVO_ANTIGO_ID, "banner.png", "image/png", 10L, AGORA.minusSeconds(60));
        when(eventoRepository.findByIdAndOrganizadorId("evento-1", "organizador-1")).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventoResponse response = service.remover("organizador-1", "evento-1");

        ArgumentCaptor<Evento> eventoCaptor = ArgumentCaptor.forClass(Evento.class);
        verify(eventoRepository).save(eventoCaptor.capture());
        Evento persistido = eventoCaptor.getValue();
        assertThat(persistido.getImagemArquivoId()).isNull();
        assertThat(persistido.getImagemArquivoNome()).isNull();
        assertThat(persistido.getImagemContentType()).isNull();
        assertThat(persistido.getImagemTamanhoBytes()).isNull();
        assertThat(persistido.getAtualizadoEm()).isEqualTo(AGORA);
        assertThat(response.imagemUrl()).isEqualTo("https://exemplo.com/banner-externo.png");
        assertThat(response.possuiImagemArquivo()).isFalse();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(gridFsTemplate).delete(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getQueryObject().get("_id"))
            .isEqualTo(new ObjectId(ARQUIVO_ANTIGO_ID));
    }

    @Test
    void removerImagemSemArquivoEnviadoRetornaEventoSemAlterarPersistencia() {
        Evento evento = eventoFuturo();
        when(eventoRepository.findByIdAndOrganizadorId("evento-1", "organizador-1")).thenReturn(Optional.of(evento));

        EventoResponse response = service.remover("organizador-1", "evento-1");

        assertThat(response.imagemUrl()).isEqualTo("https://exemplo.com/banner-externo.png");
        assertThat(response.possuiImagemArquivo()).isFalse();
        verify(eventoRepository, never()).save(any(Evento.class));
        verifyNoInteractions(gridFsTemplate);
    }

    @Test
    void buscaImagemPublicaPeloArquivoVinculadoAoEvento() {
        Evento evento = eventoFuturo();
        evento.anexarImagemArquivo(ARQUIVO_NOVO_ID, "banner.png", "image/png", 8L, AGORA.minusSeconds(60));
        GridFSFile arquivo = arquivoGridFs(ARQUIVO_NOVO_ID, "banner.png", 8L);
        GridFsResource resource = new GridFsResource(arquivo);
        when(eventoRepository.findById("evento-1")).thenReturn(Optional.of(evento));
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(arquivo);
        when(gridFsTemplate.getResource(arquivo)).thenReturn(resource);

        EventoImagemService.ImagemPublica imagem = service.buscarImagemPublica("evento-1");

        assertThat(imagem.recurso()).isSameAs(resource);
        assertThat(imagem.contentType()).isEqualTo("image/png");
        assertThat(imagem.tamanhoBytes()).isEqualTo(8L);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(gridFsTemplate).findOne(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getQueryObject().get("_id"))
            .isEqualTo(new ObjectId(ARQUIVO_NOVO_ID));
    }

    @Test
    void imagemPublicaRetornaNaoEncontradoQuandoEventoNaoPossuiArquivo() {
        Evento evento = eventoFuturo();
        when(eventoRepository.findById("evento-1")).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> service.buscarImagemPublica("evento-1"))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessage("Recurso não encontrado.");

        verifyNoInteractions(gridFsTemplate);
    }

    @Test
    void imagemPublicaRetornaNaoEncontradoQuandoArquivoNaoExisteNoGridFs() {
        Evento evento = eventoFuturo();
        evento.anexarImagemArquivo(ARQUIVO_NOVO_ID, "banner.png", "image/png", 8L, AGORA.minusSeconds(60));
        when(eventoRepository.findById("evento-1")).thenReturn(Optional.of(evento));
        when(gridFsTemplate.findOne(any(Query.class))).thenReturn(null);

        assertThatThrownBy(() -> service.buscarImagemPublica("evento-1"))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessage("Recurso não encontrado.");

        verify(gridFsTemplate, never()).getResource(any(GridFSFile.class));
    }

    @Test
    void rejeitaRemocaoDeEventoFinalizadoSemApagarArquivo() {
        Evento evento = eventoFinalizado();
        evento.anexarImagemArquivo(ARQUIVO_ANTIGO_ID, "banner.png", "image/png", 10L, AGORA.minusSeconds(60));
        when(eventoRepository.findByIdAndOrganizadorId("evento-1", "organizador-1")).thenReturn(Optional.of(evento));

        assertThatThrownBy(() -> service.remover("organizador-1", "evento-1"))
            .isInstanceOf(EventoFinalizadoException.class)
            .hasMessage("Eventos finalizados não podem ser alterados ou cancelados.");

        verify(eventoRepository, never()).save(any(Evento.class));
        verifyNoInteractions(gridFsTemplate);
    }

    private MockMultipartFile imagemPng(String nome, byte[] conteudo) {
        return new MockMultipartFile("arquivo", nome, "image/png", conteudo);
    }

    private GridFSFile arquivoGridFs(String id, String nome, long tamanhoBytes) {
        return new GridFSFile(
            new BsonObjectId(new ObjectId(id)),
            nome,
            tamanhoBytes,
            255 * 1024,
            Date.from(AGORA),
            new Document()
        );
    }

    private Evento eventoFuturo() {
        return evento(
            Instant.parse("2026-07-12T15:00:00Z"),
            Instant.parse("2026-07-12T18:00:00Z")
        );
    }

    private Evento eventoFinalizado() {
        return evento(
            Instant.parse("2026-07-10T15:00:00Z"),
            Instant.parse("2026-07-10T18:00:00Z")
        );
    }

    private Evento evento(Instant iniciaEm, Instant terminaEm) {
        Evento evento = new Evento(
            "organizador-1",
            "Backend Day",
            "Palestras sobre Java e Spring.",
            iniciaEm,
            terminaEm,
            "Auditório 1",
            false,
            "Tecnologia",
            80,
            "https://exemplo.com/banner-externo.png",
            AGORA.minusSeconds(3600)
        );
        evento.setId("evento-1");
        return evento;
    }
}
