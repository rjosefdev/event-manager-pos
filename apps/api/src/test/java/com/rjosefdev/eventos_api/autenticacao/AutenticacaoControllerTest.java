package com.rjosefdev.eventos_api.autenticacao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.rjosefdev.eventos_api.config.ApiExceptionHandler;
import com.rjosefdev.eventos_api.config.ErroSegurancaHandler;
import com.rjosefdev.eventos_api.config.SegurancaConfig;
import com.rjosefdev.eventos_api.usuarios.Perfil;

@WebMvcTest(AutenticacaoController.class)
@Import({ SegurancaConfig.class, ApiExceptionHandler.class, ErroSegurancaHandler.class })
@TestPropertySource(properties = "app.security.jwt.secret=12345678901234567890123456789012")
class AutenticacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CadastroParticipanteService service;

    @MockitoBean
    private LoginService loginService;

    @Test
    void cadastroPublicoRetornaSomenteDadosPublicosSemAceitarPerfilDoCliente() throws Exception {
        when(service.cadastrar(any())).thenReturn(
            new ParticipanteResponse("usuario-1", "Maria", "maria@exemplo.com", Perfil.PARTICIPANTE)
        );

        mockMvc.perform(post("/autenticacao/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nome": "Maria",
                      "email": "maria@exemplo.com",
                      "senha": "12345678",
                      "perfil": "ORGANIZADOR"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/usuarios/usuario-1"))
            .andExpect(jsonPath("$.id").value("usuario-1"))
            .andExpect(jsonPath("$.perfil").value("PARTICIPANTE"))
            .andExpect(jsonPath("$.senha").doesNotExist())
            .andExpect(jsonPath("$.senhaHash").doesNotExist())
            .andExpect(jsonPath("$.tokenAcesso").doesNotExist());
    }

    @Test
    void validacaoRetornaProblemDetailComErrosPorCampo() throws Exception {
        mockMvc.perform(post("/autenticacao/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "nome": "", "email": "invalido", "senha": "curta" }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Dados inválidos"))
            .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
            .andExpect(jsonPath("$.erros.nome").exists())
            .andExpect(jsonPath("$.erros.email").exists())
            .andExpect(jsonPath("$.erros.senha").exists());
    }

    @Test
    void emailDuplicadoRetornaContratoProblemDetail() throws Exception {
        when(service.cadastrar(any())).thenThrow(new EmailJaCadastradoException());

        mockMvc.perform(post("/autenticacao/cadastro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "nome": "Maria", "email": "maria@exemplo.com", "senha": "12345678" }
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.codigo").value("EMAIL_JA_CADASTRADO"))
            .andExpect(jsonPath("$.instance").value("/autenticacao/cadastro"));
    }

    @Test
    void loginPublicoRetornaTokenBearerEDadosPublicosDoUsuario() throws Exception {
        when(loginService.autenticar(any())).thenReturn(
            new LoginResponse(
                "jwt-assinado",
                "Bearer",
                java.time.Instant.parse("2026-07-11T16:00:00Z"),
                new UsuarioResumoResponse("usuario-1", "Maria", "maria@exemplo.com", Perfil.PARTICIPANTE)
            )
        );

        mockMvc.perform(post("/autenticacao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "maria@exemplo.com", "senha": "12345678" }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenAcesso").value("jwt-assinado"))
            .andExpect(jsonPath("$.tipoToken").value("Bearer"))
            .andExpect(jsonPath("$.expiraEm").value("2026-07-11T16:00:00Z"))
            .andExpect(jsonPath("$.usuario.id").value("usuario-1"))
            .andExpect(jsonPath("$.usuario.perfil").value("PARTICIPANTE"))
            .andExpect(jsonPath("$.senha").doesNotExist())
            .andExpect(jsonPath("$.senhaHash").doesNotExist())
            .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist());
    }

    @Test
    void loginComCredenciaisInvalidasRetornaProblemDetailGenerico() throws Exception {
        when(loginService.autenticar(any())).thenThrow(new CredenciaisInvalidasException());

        mockMvc.perform(post("/autenticacao/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "email": "maria@exemplo.com", "senha": "senha errada" }
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("Credenciais inválidas"))
            .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."))
            .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"))
            .andExpect(jsonPath("$.instance").value("/autenticacao/login"));
    }
}
