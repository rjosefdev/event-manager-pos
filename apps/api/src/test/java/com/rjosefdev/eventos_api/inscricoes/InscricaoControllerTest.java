package com.rjosefdev.eventos_api.inscricoes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.rjosefdev.eventos_api.catalogo.CatalogoEventoResponse;
import com.rjosefdev.eventos_api.config.ApiExceptionHandler;
import com.rjosefdev.eventos_api.config.ErroSegurancaHandler;
import com.rjosefdev.eventos_api.config.SegurancaConfig;
import com.rjosefdev.eventos_api.eventos.RecursoNaoEncontradoException;
import com.rjosefdev.eventos_api.eventos.SituacaoTemporalEvento;

@WebMvcTest(InscricaoController.class)
@Import({ SegurancaConfig.class, ApiExceptionHandler.class, ErroSegurancaHandler.class })
@TestPropertySource(properties = "app.security.jwt.secret=12345678901234567890123456789012")
class InscricaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InscricaoService inscricaoService;

    @Test
    void participanteCriaInscricaoComParticipanteIdDerivadoDoJwt() throws Exception {
        when(inscricaoService.criar(eq("participante-1"), any())).thenReturn(
            resultado(response("inscricao-1", "evento-1", "participante-1", SituacaoInscricao.ATIVA, null), true)
        );

        mockMvc.perform(post("/inscricoes")
                .with(jwtParticipante("participante-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "eventoId": "evento-1",
                      "participanteId": "participante-forjado"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/inscricoes/inscricao-1"))
            .andExpect(jsonPath("$.id").value("inscricao-1"))
            .andExpect(jsonPath("$.eventoId").value("evento-1"))
            .andExpect(jsonPath("$.participanteId").value("participante-1"))
            .andExpect(jsonPath("$.situacao").value("ATIVA"))
            .andExpect(jsonPath("$.senhaHash").doesNotExist());

        verify(inscricaoService).criar(eq("participante-1"), any());
    }

    @Test
    void participanteRecebeOkQuandoPostReutilizaInscricaoExistente() throws Exception {
        when(inscricaoService.criar(eq("participante-1"), any())).thenReturn(
            resultado(response("inscricao-1", "evento-1", "participante-1", SituacaoInscricao.ATIVA, null), false)
        );

        mockMvc.perform(post("/inscricoes")
                .with(jwtParticipante("participante-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventoId\":\"evento-1\"}"))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Location"))
            .andExpect(jsonPath("$.id").value("inscricao-1"))
            .andExpect(jsonPath("$.situacao").value("ATIVA"));

        verify(inscricaoService).criar(eq("participante-1"), any());
    }

    @Test
    void participanteListaPropriasInscricoes() throws Exception {
        when(inscricaoService.listar("participante-1")).thenReturn(List.of(
            response("inscricao-1", "evento-1", "participante-1", SituacaoInscricao.ATIVA, null),
            response("inscricao-2", "evento-2", "participante-1", SituacaoInscricao.CANCELADA, Instant.parse("2026-07-11T15:30:00Z"))
        ));

        mockMvc.perform(get("/inscricoes").with(jwtParticipante("participante-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].participanteId").value("participante-1"))
            .andExpect(jsonPath("$[0].evento.titulo").value("Backend Day"))
            .andExpect(jsonPath("$[1].situacao").value("CANCELADA"))
            .andExpect(jsonPath("$[1].canceladoEm").value("2026-07-11T15:30:00Z"));

        verify(inscricaoService).listar("participante-1");
    }

    @Test
    void participanteCancelaEReativaPropriaInscricao() throws Exception {
        when(inscricaoService.cancelar("participante-1", "inscricao-1")).thenReturn(
            response("inscricao-1", "evento-1", "participante-1", SituacaoInscricao.CANCELADA, Instant.parse("2026-07-11T15:00:00Z"))
        );
        when(inscricaoService.reativar("participante-1", "inscricao-1")).thenReturn(
            response("inscricao-1", "evento-1", "participante-1", SituacaoInscricao.ATIVA, null)
        );

        mockMvc.perform(delete("/inscricoes/inscricao-1").with(jwtParticipante("participante-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("CANCELADA"))
            .andExpect(jsonPath("$.canceladoEm").value("2026-07-11T15:00:00Z"));

        mockMvc.perform(patch("/inscricoes/inscricao-1/reativar").with(jwtParticipante("participante-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("ATIVA"))
            .andExpect(jsonPath("$.canceladoEm").value(nullValue()));

        verify(inscricaoService).cancelar("participante-1", "inscricao-1");
        verify(inscricaoService).reativar("participante-1", "inscricao-1");
    }

    @Test
    void organizadorRecebeAcessoNegadoEmInscricoesDeParticipante() throws Exception {
        mockMvc.perform(get("/inscricoes").with(jwtOrganizador("organizador-1")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        mockMvc.perform(post("/inscricoes")
                .with(jwtOrganizador("organizador-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventoId\":\"evento-1\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        mockMvc.perform(delete("/inscricoes/inscricao-1").with(jwtOrganizador("organizador-1")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        mockMvc.perform(patch("/inscricoes/inscricao-1/reativar").with(jwtOrganizador("organizador-1")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        verifyNoInteractions(inscricaoService);
    }

    @Test
    void inscricaoInexistenteOuDeOutroParticipanteRetornaNaoEncontrado() throws Exception {
        when(inscricaoService.cancelar("participante-1", "inscricao-fora"))
            .thenThrow(new RecursoNaoEncontradoException());

        mockMvc.perform(delete("/inscricoes/inscricao-fora").with(jwtParticipante("participante-1")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void erroDeRegraDeInscricaoRetornaProblemDetail() throws Exception {
        when(inscricaoService.criar(eq("participante-1"), any()))
            .thenThrow(new InscricaoNaoPermitidaException("Não há vagas disponíveis para este Evento."));

        mockMvc.perform(post("/inscricoes")
                .with(jwtParticipante("participante-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventoId\":\"evento-1\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Inscrição não permitida"))
            .andExpect(jsonPath("$.codigo").value("INSCRICAO_NAO_PERMITIDA"));
    }

    @Test
    void inscricoesSemJwtRetornamNaoAutenticado() throws Exception {
        mockMvc.perform(get("/inscricoes"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    private InscricaoResponse response(
        String id,
        String eventoId,
        String participanteId,
        SituacaoInscricao situacao,
        Instant canceladoEm
    ) {
        return new InscricaoResponse(
            id,
            eventoId,
            participanteId,
            situacao,
            Instant.parse("2026-07-11T14:00:00Z"),
            canceladoEm,
            new CatalogoEventoResponse(
                eventoId,
                "Backend Day",
                "Palestras sobre Java e Spring.",
                Instant.parse("2026-07-12T15:00:00Z"),
                Instant.parse("2026-07-12T18:00:00Z"),
                "Auditório 1",
                false,
                "Tecnologia",
                80,
                null,
                false,
                SituacaoTemporalEvento.FUTURO,
                20,
                true
            )
        );
    }

    private ResultadoCriacaoInscricao resultado(InscricaoResponse inscricao, boolean criada) {
        return new ResultadoCriacaoInscricao(inscricao, criada);
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
