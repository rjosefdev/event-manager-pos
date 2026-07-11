package com.rjosefdev.eventos_api.inscricoes;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
public class IndiceInscricoesConfig implements ApplicationRunner {

    public static final String NOME_INDICE_EVENTO_PARTICIPANTE = "uk_inscricoes_evento_participante";

    private final MongoTemplate mongoTemplate;

    public IndiceInscricoesConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        mongoTemplate.indexOps(Inscricao.class).createIndex(
            new Index()
                .on("eventoId", Direction.ASC)
                .on("participanteId", Direction.ASC)
                .unique()
                .named(NOME_INDICE_EVENTO_PARTICIPANTE)
        );
    }
}
