package com.rjosefdev.eventos_api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.mongodb.autoconfigure.MongoProperties;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

class MongoConfiguracaoTest {

    @Test
    void importaEnvOpcionalDaRaizEDeAppsApi() throws IOException {
        Properties propriedades = new Properties();
        propriedades.load(new ClassPathResource("application.properties").getInputStream());

        assertThat(propriedades.getProperty("spring.config.import"))
            .isEqualTo("optional:file:.env[.properties],optional:file:apps/api/.env[.properties]");
    }

    @Test
    void defineDatabaseMesmoQuandoUriNaoContemNomeDoBanco() throws IOException {
        Properties propriedades = new Properties();
        propriedades.load(new ClassPathResource("application.properties").getInputStream());

        StandardEnvironment ambiente = new StandardEnvironment();
        ambiente.getPropertySources().addFirst(
            new PropertiesPropertySource("application.properties", propriedades)
        );
        ambiente.getPropertySources().addFirst(
            new MapPropertySource("teste", Map.of(
                "MONGODB_URI", "mongodb://localhost:27017/?retryWrites=true"
            ))
        );

        MongoProperties mongo = Binder.get(ambiente)
            .bind("spring.mongodb", Bindable.of(MongoProperties.class))
            .orElseThrow(() -> new IllegalStateException("Configuração do MongoDB não encontrada"));

        assertThat(mongo.getDatabase()).isEqualTo("event_manager_pos");
    }
}
