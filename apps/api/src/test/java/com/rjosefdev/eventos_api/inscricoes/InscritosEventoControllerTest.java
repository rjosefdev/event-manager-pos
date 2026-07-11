package com.rjosefdev.eventos_api.inscricoes;

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

@WebMvcTest(InscritosEventoController.class)
@Import({ SegurancaConfig.class, ApiExceptionHandler.class, ErroSegurancaHandler.class })
@TestPropertySource(properties = "app.security.jwt.secret=12345678901234567890123456789012")
class InscritosEventoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InscritosEventoService inscritosEventoService;

    @Test
    void organizadorListaInscritosDoEventoProprio() throws Exception {
        when(inscritosEventoService.listarDoEvento("organizador-1", "evento-1")).thenReturn(List.of(
            response("inscricao-1", "participante-1", SituacaoInscricao.ATIVA, null, "Ana Lima", "ana@email.com"),
            response("inscricao-2", "participante-2", SituacaoInscricao.CANCELADA, Instant.parse("2026-07-11T15:30:00Z"), "Bruno Dias", "bruno@email.com")
        ));

        mockMvc.perform(get("/eventos/evento-1/inscricoes").with(jwtOrganizador("organizador-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value("inscricao-1"))
            .andExpect(jsonPath("$[0].eventoId").value("evento-1"))
            .andExpect(jsonPath("$[0].participanteId").value("participante-1"))
            .andExpect(jsonPath("$[0].situacao").value("ATIVA"))
            .andExpect(jsonPath("$[0].participante.nome").value("Ana Lima"))
            .andExpect(jsonPath("$[0].participante.email").value("ana@email.com"))
            .andExpect(jsonPath("$[0].participante.senhaHash").doesNotExist())
            .andExpect(jsonPath("$[0].senhaHash").doesNotExist())
            .andExpect(jsonPath("$[1].situacao").value("CANCELADA"))
            .andExpect(jsonPath("$[1].canceladoEm").value("2026-07-11T15:30:00Z"));

        verify(inscritosEventoService).listarDoEvento("organizador-1", "evento-1");
    }

    @Test
    void organizadorRecebeListaVaziaQuandoEventoNaoPossuiInscritos() throws Exception {
        when(inscritosEventoService.listarDoEvento("organizador-1", "evento-1")).thenReturn(List.of());

        mockMvc.perform(get("/eventos/evento-1/inscricoes").with(jwtOrganizador("organizador-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void eventoInexistenteOuDeOutroOrganizadorRetornaNaoEncontrado() throws Exception {
        when(inscritosEventoService.listarDoEvento("organizador-1", "evento-fora"))
            .thenThrow(new RecursoNaoEncontradoException());

        mockMvc.perform(get("/eventos/evento-fora/inscricoes").with(jwtOrganizador("organizador-1")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
            .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void participanteRecebeAcessoNegadoAoConsultarInscritos() throws Exception {
        mockMvc.perform(get("/eventos/evento-1/inscricoes").with(jwtParticipante("participante-1")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        verifyNoInteractions(inscritosEventoService);
    }

    @Test
    void consultaInscritosSemJwtRetornaNaoAutenticado() throws Exception {
        mockMvc.perform(get("/eventos/evento-1/inscricoes"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    private InscritoEventoResponse response(
        String id,
        String participanteId,
        SituacaoInscricao situacao,
        Instant canceladoEm,
        String nome,
        String email
    ) {
        return new InscritoEventoResponse(
            id,
            "evento-1",
            participanteId,
            situacao,
            Instant.parse("2026-07-11T14:00:00Z"),
            canceladoEm,
            new ParticipanteInscritoResponse(participanteId, nome, email)
        );
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor jwtOrganizador(String sub) {
        return jwt()
            .jwt(token -> token.subject(sub).claim("perfil", "ORGANIZADOR"))
            .authorities(new SimpleGrantedAuthority("ROLE_ORGANIZADOR"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor jwtParticipante(String sub) {
        return jwt()
            .jwt(token -> token.subject(sub).claim("perfil", "PARTICIPANTE"))
            .authorities(new SimpleGrantedAuthority("ROLE_PARTICIPANTE"));
    }
}
