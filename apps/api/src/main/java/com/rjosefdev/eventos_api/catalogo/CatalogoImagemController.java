package com.rjosefdev.eventos_api.catalogo;

import java.time.Duration;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rjosefdev.eventos_api.eventos.EventoImagemService;
import com.rjosefdev.eventos_api.eventos.EventoImagemService.ImagemPublica;

@RestController
@RequestMapping("/catalogo/eventos")
public class CatalogoImagemController {

    private final EventoImagemService eventoImagemService;

    public CatalogoImagemController(EventoImagemService eventoImagemService) {
        this.eventoImagemService = eventoImagemService;
    }

    @GetMapping("/{id}/imagem")
    public ResponseEntity<Resource> buscarImagem(@PathVariable String id) {
        ImagemPublica imagem = eventoImagemService.buscarImagemPublica(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(imagem.contentType()));
        headers.setCacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic());
        if (imagem.tamanhoBytes() != null) {
            headers.setContentLength(imagem.tamanhoBytes());
        }

        return ResponseEntity
            .ok()
            .headers(headers)
            .body(imagem.recurso());
    }
}
