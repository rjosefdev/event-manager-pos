package com.rjosefdev.eventos_api.catalogo;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.rjosefdev.eventos_api.config.ApiExceptionHandler;
import com.rjosefdev.eventos_api.config.ErroSegurancaHandler;
import com.rjosefdev.eventos_api.config.SegurancaConfig;
import com.rjosefdev.eventos_api.eventos.RecursoNaoEncontradoException;
import com.rjosefdev.eventos_api.eventos.SituacaoTemporalEvento;

@WebMvcTest(CatalogoController.class)
@Import({ SegurancaConfig.class, ApiExceptionHandler.class, ErroSegurancaHandler.class })
@TestPropertySource(properties = "app.security.jwt.secret=12345678901234567890123456789012")
class CatalogoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogoService catalogoService;

    @Test
    void participanteListaCatalogo() throws Exception {
        when(catalogoService.listar(CatalogoFiltro.semFiltros())).thenReturn(List.of(
            response("evento-futuro", "Backend Day", false, SituacaoTemporalEvento.FUTURO, 10, true),
            response("evento-lotado", "Frontend Summit", false, SituacaoTemporalEvento.FUTURO, 0, false),
            response("evento-cancelado", "Design Week", true, SituacaoTemporalEvento.FUTURO, 7, false)
        ));

        mockMvc.perform(get("/catalogo/eventos").with(jwtParticipante("participante-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").value("evento-futuro"))
            .andExpect(jsonPath("$[0].situacaoTemporal").value("FUTURO"))
            .andExpect(jsonPath("$[0].vagasDisponiveis").value(10))
            .andExpect(jsonPath("$[0].inscricaoPermitida").value(true))
            .andExpect(jsonPath("$[1].vagasDisponiveis").value(0))
            .andExpect(jsonPath("$[1].inscricaoPermitida").value(false))
            .andExpect(jsonPath("$[2].cancelado").value(true))
            .andExpect(jsonPath("$[2].inscricaoPermitida").value(false));

        verify(catalogoService).listar(CatalogoFiltro.semFiltros());
    }

    @Test
    void participanteListaCatalogoComBuscaCategoriaEOrdem() throws Exception {
        CatalogoFiltro filtro = new CatalogoFiltro("spring", "Tecnologia", "desc");
        when(catalogoService.listar(filtro)).thenReturn(List.of(
            response("evento-1", "Spring Night", false, SituacaoTemporalEvento.FUTURO, 4, true)
        ));

        mockMvc.perform(get("/catalogo/eventos")
                .queryParam("busca", "spring")
                .queryParam("categoria", "Tecnologia")
                .queryParam("ordem", "desc")
                .with(jwtParticipante("participante-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value("evento-1"));

        verify(catalogoService).listar(filtro);
    }

    @Test
    void participanteConsultaDetalhesDoCatalogo() throws Exception {
        when(catalogoService.buscarDetalhes("evento-1")).thenReturn(
            response("evento-1", "Backend Day", false, SituacaoTemporalEvento.EM_ANDAMENTO, 3, true)
        );

        mockMvc.perform(get("/catalogo/eventos/evento-1").with(jwtParticipante("participante-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("evento-1"))
            .andExpect(jsonPath("$.titulo").value("Backend Day"))
            .andExpect(jsonPath("$.situacaoTemporal").value("EM_ANDAMENTO"))
            .andExpect(jsonPath("$.vagasDisponiveis").value(3))
            .andExpect(jsonPath("$.inscricaoPermitida").value(true))
            .andExpect(jsonPath("$.senhaHash").doesNotExist());

        verify(catalogoService).buscarDetalhes("evento-1");
    }

    @Test
    void organizadorRecebeAcessoNegadoNoCatalogoEDetalhes() throws Exception {
        mockMvc.perform(get("/catalogo/eventos").with(jwtOrganizador("organizador-1")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        mockMvc.perform(get("/catalogo/eventos/evento-1").with(jwtOrganizador("organizador-1")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        verifyNoInteractions(catalogoService);
    }

    @Test
    void catalogoSemJwtRetornaNaoAutenticado() throws Exception {
        mockMvc.perform(get("/catalogo/eventos"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void eventoInexistenteRetornaNaoEncontrado() throws Exception {
        when(catalogoService.buscarDetalhes("evento-inexistente"))
            .thenThrow(new RecursoNaoEncontradoException());

        mockMvc.perform(get("/catalogo/eventos/evento-inexistente").with(jwtParticipante("participante-1")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
            .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    private CatalogoEventoResponse response(
        String id,
        String titulo,
        boolean cancelado,
        SituacaoTemporalEvento situacaoTemporal,
        int vagasDisponiveis,
        boolean inscricaoPermitida
    ) {
        return new CatalogoEventoResponse(
            id,
            titulo,
            "Palestras sobre Java e Spring.",
            Instant.parse("2026-07-12T15:00:00Z"),
            Instant.parse("2026-07-12T18:00:00Z"),
            "Auditório 1",
            false,
            "Tecnologia",
            80,
            "https://exemplo.com/evento.png",
            false,
            cancelado,
            situacaoTemporal,
            vagasDisponiveis,
            inscricaoPermitida
        );
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor jwtParticipante(String sub) {
        return jwt()
            .jwt(token -> token.subject(sub).claim("perfil", "PARTICIPANTE"))
            .authorities(new SimpleGrantedAuthority("ROLE_PARTICIPANTE"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor jwtOrganizador(String sub) {
        return jwt()
            .jwt(token -> token.subject(sub).claim("perfil", "ORGANIZADOR"))
            .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZADOR"));
    }
}
