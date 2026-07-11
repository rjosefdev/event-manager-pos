package com.rjosefdev.eventos_api.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mongodb.client.MongoClient;
import com.rjosefdev.eventos_api.inscricoes.IndiceInscricoesConfig;
import com.rjosefdev.eventos_api.usuarios.IndiceUsuariosConfig;

@SpringBootTest(properties = {
    "app.security.jwt.secret=12345678901234567890123456789012",
    "spring.mongodb.uri=mongodb://localhost:27017/event_manager_pos_test"
})
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IndiceUsuariosConfig indiceUsuariosConfig;

    @MockitoBean
    private IndiceInscricoesConfig indiceInscricoesConfig;

    @MockitoBean
    private MongoClient mongoClient;

    @Test
    void apiDocsExpoeBearerJwtParaAutorizarRotasProtegidasNoSwagger() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].type").value("http"))
            .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].scheme").value("bearer"))
            .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].bearerFormat").value("JWT"))
            .andExpect(jsonPath("$.paths['/usuarios/atual'].get.security[0]['bearer-jwt']").isArray())
            .andExpect(jsonPath("$.paths['/autenticacao/login'].post.security").doesNotExist());
    }
}
