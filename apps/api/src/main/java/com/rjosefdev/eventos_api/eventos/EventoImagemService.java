package com.rjosefdev.eventos_api.eventos;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EventoImagemService {

    private static final long TAMANHO_MAXIMO_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of(
        "image/jpeg",
        "image/png",
        "image/webp"
    );

    private final EventoRepository eventoRepository;
    private final GridFsTemplate gridFsTemplate;
    private final Clock clock;

    public EventoImagemService(
        EventoRepository eventoRepository,
        GridFsTemplate gridFsTemplate,
        Clock clock
    ) {
        this.eventoRepository = eventoRepository;
        this.gridFsTemplate = gridFsTemplate;
        this.clock = clock;
    }

    public EventoResponse anexar(String organizadorId, String eventoId, MultipartFile arquivo) {
        validarArquivo(arquivo);
        Instant agora = clock.instant();
        Evento evento = buscarEventoProprioEditavel(organizadorId, eventoId, agora);
        String arquivoAntigoId = evento.getImagemArquivoId();

        ImagemArmazenada imagem = armazenar(evento, arquivo);
        evento.anexarImagemArquivo(
            imagem.id(),
            imagem.nome(),
            imagem.contentType(),
            imagem.tamanhoBytes(),
            agora
        );

        Evento eventoSalvo;
        try {
            eventoSalvo = eventoRepository.save(evento);
        } catch (RuntimeException exception) {
            removerArquivoSeExistir(imagem.id());
            throw exception;
        }

        removerArquivoSeExistir(arquivoAntigoId);
        return EventoResponse.de(eventoSalvo, agora);
    }

    private Evento buscarEventoProprioEditavel(String organizadorId, String eventoId, Instant agora) {
        Evento evento = eventoRepository.findByIdAndOrganizadorId(eventoId, organizadorId)
            .orElseThrow(RecursoNaoEncontradoException::new);
        if (!agora.isBefore(evento.getTerminaEm())) {
            throw new EventoFinalizadoException();
        }
        return evento;
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new EventoImagemInvalidaException("Informe um arquivo de imagem.");
        }
        String contentType = arquivo.getContentType();
        if (contentType == null || !CONTENT_TYPES_PERMITIDOS.contains(contentType)) {
            throw new EventoImagemInvalidaException("Tipo de imagem não permitido.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new EventoImagemInvalidaException("A imagem deve ter no máximo 5 MB.");
        }
    }

    private ImagemArmazenada armazenar(Evento evento, MultipartFile arquivo) {
        String contentType = arquivo.getContentType();
        String nomeOriginal = nomeOriginalOuPadrao(arquivo);
        Document metadata = new Document()
            .append("eventoId", evento.getId())
            .append("organizadorId", evento.getOrganizadorId())
            .append("contentType", contentType)
            .append("originalFilename", nomeOriginal)
            .append("tamanhoBytes", arquivo.getSize());

        try (InputStream inputStream = arquivo.getInputStream()) {
            ObjectId arquivoId = gridFsTemplate.store(inputStream, nomeOriginal, contentType, metadata);
            return new ImagemArmazenada(arquivoId.toHexString(), nomeOriginal, contentType, arquivo.getSize());
        } catch (IOException exception) {
            throw new EventoImagemInvalidaException("Não foi possível ler o arquivo de imagem.");
        }
    }

    private String nomeOriginalOuPadrao(MultipartFile arquivo) {
        String nomeOriginal = arquivo.getOriginalFilename();
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return "imagem-evento";
        }
        return nomeOriginal;
    }

    private void removerArquivoSeExistir(String arquivoId) {
        if (arquivoId == null || arquivoId.isBlank()) {
            return;
        }
        gridFsTemplate.delete(Query.query(Criteria.where("_id").is(valorIdGridFs(arquivoId))));
    }

    private Object valorIdGridFs(String arquivoId) {
        if (ObjectId.isValid(arquivoId)) {
            return new ObjectId(arquivoId);
        }
        return arquivoId;
    }

    private record ImagemArmazenada(
        String id,
        String nome,
        String contentType,
        long tamanhoBytes
    ) {
    }
}
