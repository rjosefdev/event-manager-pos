package com.rjosefdev.eventos_api.eventos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.rjosefdev.eventos_api.config.ApiExceptionHandler;
import com.rjosefdev.eventos_api.config.ErroSegurancaHandler;
import com.rjosefdev.eventos_api.config.SegurancaConfig;

@WebMvcTest(EventoController.class)
@Import({ SegurancaConfig.class, ApiExceptionHandler.class, ErroSegurancaHandler.class })
@TestPropertySource(properties = "app.security.jwt.secret=12345678901234567890123456789012")
class EventoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventoService eventoService;

    @Test
    void organizadorCriaEventoComOrganizadorIdDerivadoDoJwt() throws Exception {
        when(eventoService.criar(eq("organizador-1"), any())).thenReturn(
            response("evento-1", "organizador-1", "Backend Day", SituacaoTemporalEvento.FUTURO)
        );

        mockMvc.perform(post("/eventos")
                .with(jwtOrganizador("organizador-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "titulo": "Backend Day",
                      "descricao": "Palestras sobre Java e Spring.",
                      "iniciaEm": "2026-07-12T15:00:00Z",
                      "terminaEm": "2026-07-12T18:00:00Z",
                      "local": "Auditório 1",
                      "online": false,
                      "categoria": "Tecnologia",
                      "vagas": 80,
                      "imagemUrl": "https://exemplo.com/evento.png",
                      "organizadorId": "organizador-forjado"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/eventos/evento-1"))
            .andExpect(jsonPath("$.id").value("evento-1"))
            .andExpect(jsonPath("$.organizadorId").value("organizador-1"))
            .andExpect(jsonPath("$.situacaoTemporal").value("FUTURO"))
            .andExpect(jsonPath("$.senhaHash").doesNotExist());

        verify(eventoService).criar(eq("organizador-1"), any());
    }

    @Test
    void organizadorListaEventosProprios() throws Exception {
        when(eventoService.listarDoOrganizador("organizador-1")).thenReturn(List.of(
            response("evento-1", "organizador-1", "Backend Day", SituacaoTemporalEvento.FUTURO),
            response("evento-2", "organizador-1", "Spring Night", SituacaoTemporalEvento.EM_ANDAMENTO)
        ));

        mockMvc.perform(get("/eventos").with(jwtOrganizador("organizador-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].organizadorId").value("organizador-1"))
            .andExpect(jsonPath("$[1].organizadorId").value("organizador-1"))
            .andExpect(jsonPath("$[0].situacaoTemporal").value("FUTURO"))
            .andExpect(jsonPath("$[1].situacaoTemporal").value("EM_ANDAMENTO"));

        verify(eventoService).listarDoOrganizador("organizador-1");
    }

    @Test
    void organizadorConsultaEventoProprioPorId() throws Exception {
        when(eventoService.buscarDoOrganizador("organizador-1", "evento-1")).thenReturn(
            response("evento-1", "organizador-1", "Backend Day", SituacaoTemporalEvento.FUTURO)
        );

        mockMvc.perform(get("/eventos/evento-1").with(jwtOrganizador("organizador-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("evento-1"))
            .andExpect(jsonPath("$.organizadorId").value("organizador-1"))
            .andExpect(jsonPath("$.titulo").value("Backend Day"));

        verify(eventoService).buscarDoOrganizador("organizador-1", "evento-1");
    }

    @Test
    void organizadorRecebeListaVaziaQuandoNaoPossuiEventos() throws Exception {
        when(eventoService.listarDoOrganizador("organizador-1")).thenReturn(List.of());

        mockMvc.perform(get("/eventos").with(jwtOrganizador("organizador-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void participanteNaoPodeCriarNemListarEventosDeOrganizador() throws Exception {
        mockMvc.perform(post("/eventos")
                .with(jwtParticipante("participante-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "titulo": "Backend Day",
                      "descricao": "Palestras sobre Java e Spring.",
                      "iniciaEm": "2026-07-12T15:00:00Z",
                      "terminaEm": "2026-07-12T18:00:00Z",
                      "local": "Auditório 1",
                      "online": false,
                      "categoria": "Tecnologia",
                      "vagas": 80
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        mockMvc.perform(get("/eventos").with(jwtParticipante("participante-1")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        verifyNoInteractions(eventoService);
    }

    @Test
    void periodoInvalidoRetornaProblemDetail() throws Exception {
        when(eventoService.criar(eq("organizador-1"), any())).thenThrow(new PeriodoEventoInvalidoException());

        mockMvc.perform(post("/eventos")
                .with(jwtOrganizador("organizador-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "titulo": "Backend Day",
                      "descricao": "Palestras sobre Java e Spring.",
                      "iniciaEm": "2026-07-12T18:00:00Z",
                      "terminaEm": "2026-07-12T15:00:00Z",
                      "local": "Auditório 1",
                      "online": false,
                      "categoria": "Tecnologia",
                      "vagas": 80
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Período do evento inválido"))
            .andExpect(jsonPath("$.codigo").value("PERIODO_EVENTO_INVALIDO"));
    }

    @Test
    void organizadorEditaEventoProprio() throws Exception {
        when(eventoService.atualizar(eq("organizador-1"), eq("evento-1"), any())).thenReturn(
            response("evento-1", "organizador-1", "Frontend Summit", SituacaoTemporalEvento.FUTURO)
        );

        mockMvc.perform(put("/eventos/evento-1")
                .with(jwtOrganizador("organizador-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "titulo": "Frontend Summit",
                      "descricao": "Palestras sobre React e Next.",
                      "iniciaEm": "2026-07-13T15:00:00Z",
                      "terminaEm": "2026-07-13T18:00:00Z",
                      "local": "Sala 2",
                      "online": true,
                      "categoria": "Frontend",
                      "vagas": 50,
                      "imagemUrl": "https://exemplo.com/frontend.png",
                      "organizadorId": "organizador-forjado"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("evento-1"))
            .andExpect(jsonPath("$.organizadorId").value("organizador-1"))
            .andExpect(jsonPath("$.titulo").value("Frontend Summit"));

        verify(eventoService).atualizar(eq("organizador-1"), eq("evento-1"), any());
    }

    @Test
    void organizadorCancelaEventoProprio() throws Exception {
        when(eventoService.cancelar("organizador-1", "evento-1")).thenReturn(
            responseCancelado("evento-1", "organizador-1", "Backend Day", SituacaoTemporalEvento.FUTURO)
        );

        mockMvc.perform(delete("/eventos/evento-1").with(jwtOrganizador("organizador-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("evento-1"))
            .andExpect(jsonPath("$.organizadorId").value("organizador-1"))
            .andExpect(jsonPath("$.cancelado").value(true));

        verify(eventoService).cancelar("organizador-1", "evento-1");
    }

    @Test
    void eventoInexistenteOuDeOutroOrganizadorRetornaNaoEncontrado() throws Exception {
        when(eventoService.buscarDoOrganizador("organizador-1", "evento-fora"))
            .thenThrow(new RecursoNaoEncontradoException());
        when(eventoService.atualizar(eq("organizador-1"), eq("evento-fora"), any()))
            .thenThrow(new RecursoNaoEncontradoException());
        when(eventoService.cancelar("organizador-1", "evento-fora"))
            .thenThrow(new RecursoNaoEncontradoException());

        mockMvc.perform(get("/eventos/evento-fora").with(jwtOrganizador("organizador-1")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
            .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));

        mockMvc.perform(put("/eventos/evento-fora")
                .with(jwtOrganizador("organizador-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "titulo": "Frontend Summit",
                      "descricao": "Palestras sobre React e Next.",
                      "iniciaEm": "2026-07-13T15:00:00Z",
                      "terminaEm": "2026-07-13T18:00:00Z",
                      "local": "Sala 2",
                      "online": true,
                      "categoria": "Frontend",
                      "vagas": 50
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
            .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));

        mockMvc.perform(delete("/eventos/evento-fora").with(jwtOrganizador("organizador-1")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
            .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void eventoFinalizadoRetornaErroDeNegocio() throws Exception {
        when(eventoService.atualizar(eq("organizador-1"), eq("evento-1"), any()))
            .thenThrow(new EventoFinalizadoException());
        when(eventoService.cancelar("organizador-1", "evento-1"))
            .thenThrow(new EventoFinalizadoException());

        mockMvc.perform(put("/eventos/evento-1")
                .with(jwtOrganizador("organizador-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "titulo": "Frontend Summit",
                      "descricao": "Palestras sobre React e Next.",
                      "iniciaEm": "2026-07-13T15:00:00Z",
                      "terminaEm": "2026-07-13T18:00:00Z",
                      "local": "Sala 2",
                      "online": true,
                      "categoria": "Frontend",
                      "vagas": 50
                    }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Evento finalizado"))
            .andExpect(jsonPath("$.codigo").value("EVENTO_FINALIZADO"));

        mockMvc.perform(delete("/eventos/evento-1").with(jwtOrganizador("organizador-1")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Evento finalizado"))
            .andExpect(jsonPath("$.codigo").value("EVENTO_FINALIZADO"));
    }

    @Test
    void participanteNaoPodeEditarNemCancelarEvento() throws Exception {
        mockMvc.perform(put("/eventos/evento-1")
                .with(jwtParticipante("participante-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "titulo": "Frontend Summit",
                      "descricao": "Palestras sobre React e Next.",
                      "iniciaEm": "2026-07-13T15:00:00Z",
                      "terminaEm": "2026-07-13T18:00:00Z",
                      "local": "Sala 2",
                      "online": true,
                      "categoria": "Frontend",
                      "vagas": 50
                    }
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        mockMvc.perform(delete("/eventos/evento-1").with(jwtParticipante("participante-1")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    private EventoResponse response(
        String id,
        String organizadorId,
        String titulo,
        SituacaoTemporalEvento situacaoTemporal
    ) {
        return new EventoResponse(
            id,
            organizadorId,
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
            situacaoTemporal
        );
    }

    private EventoResponse responseCancelado(
        String id,
        String organizadorId,
        String titulo,
        SituacaoTemporalEvento situacaoTemporal
    ) {
        return new EventoResponse(
            id,
            organizadorId,
            titulo,
            "Palestras sobre Java e Spring.",
            Instant.parse("2026-07-12T15:00:00Z"),
            Instant.parse("2026-07-12T18:00:00Z"),
            "Auditório 1",
            false,
            "Tecnologia",
            80,
            "https://exemplo.com/evento.png",
            true,
            situacaoTemporal
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
