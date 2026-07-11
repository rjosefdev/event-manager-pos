package com.rjosefdev.eventos_api.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.rjosefdev.eventos_api.config.ApiExceptionHandler;
import com.rjosefdev.eventos_api.config.ErroSegurancaHandler;
import com.rjosefdev.eventos_api.config.SegurancaConfig;
import com.rjosefdev.eventos_api.usuarios.Perfil;

@WebMvcTest(AdminController.class)
@Import({ SegurancaConfig.class, ApiExceptionHandler.class, ErroSegurancaHandler.class })
@TestPropertySource(properties = "app.security.jwt.secret=12345678901234567890123456789012")
class AdminControllerTest {

    private static final Instant AGORA = Instant.parse("2026-07-11T15:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService service;

    @Test
    @WithMockUser(roles = "ORGANIZADOR")
    void cadastraOrganizadorRetornandoDadosPublicos() throws Exception {
        when(service.cadastrarOrganizador(any())).thenReturn(
            new UsuarioAdminResponse(
                "organizador-1",
                "Organizador",
                "organizador@exemplo.com",
                Perfil.ORGANIZADOR,
                true,
                AGORA,
                AGORA
            )
        );

        mockMvc.perform(post("/admin/organizadores")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "nome": "Organizador",
                      "email": "organizador@exemplo.com",
                      "senha": "12345678",
                      "perfil": "PARTICIPANTE"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/admin/usuarios/organizador-1"))
            .andExpect(jsonPath("$.perfil").value("ORGANIZADOR"))
            .andExpect(jsonPath("$.senha").doesNotExist())
            .andExpect(jsonPath("$.senhaHash").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ORGANIZADOR")
    void listaTodosOsUsuariosSemDadosDeSenha() throws Exception {
        when(service.listarUsuarios()).thenReturn(List.of(
            new UsuarioAdminResponse(
                "usuario-1", "Organizador", "organizador@exemplo.com",
                Perfil.ORGANIZADOR, true, AGORA, AGORA
            ),
            new UsuarioAdminResponse(
                "usuario-2", "Participante", "participante@exemplo.com",
                Perfil.PARTICIPANTE, true, AGORA, AGORA
            )
        ));

        mockMvc.perform(get("/admin/usuarios"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].perfil").value("ORGANIZADOR"))
            .andExpect(jsonPath("$[1].perfil").value("PARTICIPANTE"))
            .andExpect(jsonPath("$[0].senhaHash").doesNotExist());
    }

    @Test
    void endpointsAdminExigemAutenticacao() throws Exception {
        mockMvc.perform(get("/admin/usuarios"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    @WithMockUser(roles = "PARTICIPANTE")
    void perfilIncompativelRetornaAcessoNegado() throws Exception {
        mockMvc.perform(get("/admin/usuarios"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.title").value("Acesso negado"))
            .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        verifyNoInteractions(service);
    }
}
