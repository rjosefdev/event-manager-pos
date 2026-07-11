package com.rjosefdev.eventos_api.catalogo;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.rjosefdev.eventos_api.config.ApiExceptionHandler;
import com.rjosefdev.eventos_api.config.ErroSegurancaHandler;
import com.rjosefdev.eventos_api.config.SegurancaConfig;
import com.rjosefdev.eventos_api.eventos.EventoImagemService;
import com.rjosefdev.eventos_api.eventos.EventoImagemService.ImagemPublica;
import com.rjosefdev.eventos_api.eventos.RecursoNaoEncontradoException;

@WebMvcTest(CatalogoImagemController.class)
@Import({ SegurancaConfig.class, ApiExceptionHandler.class, ErroSegurancaHandler.class })
@TestPropertySource(properties = "app.security.jwt.secret=12345678901234567890123456789012")
class CatalogoImagemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventoImagemService eventoImagemService;

    @Test
    void leituraPublicaRetornaBytesDaImagemSemJwt() throws Exception {
        byte[] bytes = "conteudo".getBytes();
        when(eventoImagemService.buscarImagemPublica("evento-1")).thenReturn(
            new ImagemPublica(new ByteArrayResource(bytes), "image/png", 8L)
        );

        mockMvc.perform(get("/catalogo/eventos/evento-1/imagem"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/png"))
            .andExpect(header().longValue("Content-Length", 8L))
            .andExpect(header().string("Cache-Control", "max-age=3600, public"))
            .andExpect(content().bytes(bytes));

        verify(eventoImagemService).buscarImagemPublica("evento-1");
    }

    @Test
    void leituraPublicaRetornaNaoEncontradoQuandoEventoNaoPossuiImagem() throws Exception {
        when(eventoImagemService.buscarImagemPublica("evento-sem-imagem"))
            .thenThrow(new RecursoNaoEncontradoException());

        mockMvc.perform(get("/catalogo/eventos/evento-sem-imagem/imagem"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
            .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));

        verify(eventoImagemService).buscarImagemPublica("evento-sem-imagem");
    }
}
