package com.rjosefdev.eventos_api.inscricoes;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;

class IndiceInscricoesConfigTest {

    @Test
    void criaIndiceUnicoPorEventoEParticipanteNaInicializacao() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        IndexOperations indexOperations = mock(IndexOperations.class);
        when(mongoTemplate.indexOps(Inscricao.class)).thenReturn(indexOperations);

        new IndiceInscricoesConfig(mongoTemplate).run(null);

        verify(indexOperations).createIndex(argThat(index ->
            index.getIndexOptions().get("unique").equals(true)
                && IndiceInscricoesConfig.NOME_INDICE_EVENTO_PARTICIPANTE.equals(index.getIndexOptions().get("name"))
                && index.getIndexKeys().containsKey("eventoId")
                && index.getIndexKeys().containsKey("participanteId")
        ));
    }
}
