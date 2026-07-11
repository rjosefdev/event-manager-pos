package com.rjosefdev.eventos_api.usuarios;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
public class IndiceUsuariosConfig implements ApplicationRunner {

    public static final String NOME_INDICE_EMAIL = "uk_usuarios_email";

    private final MongoTemplate mongoTemplate;

    public IndiceUsuariosConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        mongoTemplate.indexOps(Usuario.class).createIndex(
            new Index().on("email", Direction.ASC).unique().named(NOME_INDICE_EMAIL)
        );
    }
}
