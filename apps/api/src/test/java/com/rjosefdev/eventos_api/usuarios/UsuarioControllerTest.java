package com.rjosefdev.eventos_api.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.rjosefdev.eventos_api.autenticacao.UsuarioResumoResponse;
import com.rjosefdev.eventos_api.config.ApiExceptionHandler;
import com.rjosefdev.eventos_api.config.ErroSegurancaHandler;
import com.rjosefdev.eventos_api.config.SegurancaConfig;

@WebMvcTest(UsuarioController.class)
@Import({ SegurancaConfig.class, ApiExceptionHandler.class, ErroSegurancaHandler.class })
@TestPropertySource(properties = "app.security.jwt.secret=12345678901234567890123456789012")
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioAtualService usuarioAtualService;

    @Test
    void usuarioAtualRetornaParticipantePeloSubDoJwt() throws Exception {
        when(usuarioAtualService.buscarPorSub("participante-1")).thenReturn(
            new UsuarioResumoResponse("participante-1", "Participante", "participante@exemplo.com", Perfil.PARTICIPANTE)
        );

        mockMvc.perform(get("/usuarios/atual")
                .with(jwt().jwt(token -> token.subject("participante-1").claim("perfil", "PARTICIPANTE"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("participante-1"))
            .andExpect(jsonPath("$.nome").value("Participante"))
            .andExpect(jsonPath("$.email").value("participante@exemplo.com"))
            .andExpect(jsonPath("$.perfil").value("PARTICIPANTE"))
            .andExpect(jsonPath("$.senha").doesNotExist())
            .andExpect(jsonPath("$.senhaHash").doesNotExist());

        verify(usuarioAtualService).buscarPorSub("participante-1");
    }

    @Test
    void usuarioAtualRetornaOrganizadorPeloSubDoJwt() throws Exception {
        when(usuarioAtualService.buscarPorSub("organizador-1")).thenReturn(
            new UsuarioResumoResponse("organizador-1", "Organizador", "organizador@exemplo.com", Perfil.ORGANIZADOR)
        );

        mockMvc.perform(get("/usuarios/atual")
                .with(jwt().jwt(token -> token.subject("organizador-1").claim("perfil", "ORGANIZADOR"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("organizador-1"))
            .andExpect(jsonPath("$.perfil").value("ORGANIZADOR"));

        verify(usuarioAtualService).buscarPorSub("organizador-1");
    }

    @Test
    void rotaProtegidaSemTokenRetornaProblemDetail() throws Exception {
        mockMvc.perform(get("/usuarios/atual"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("Não autenticado"))
            .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"))
            .andExpect(jsonPath("$.instance").value("/usuarios/atual"));
    }

    @Test
    void rotaProtegidaComTokenInvalidoRetornaProblemDetail() throws Exception {
        mockMvc.perform(get("/usuarios/atual")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("Não autenticado"))
            .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    void preflightPassaSemJwtComCorsAbertoESemCredenciais() throws Exception {
        mockMvc.perform(options("/usuarios/atual")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void swaggerOpenApiNaoExigemJwtEActuatorExigeJwt() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }
}
