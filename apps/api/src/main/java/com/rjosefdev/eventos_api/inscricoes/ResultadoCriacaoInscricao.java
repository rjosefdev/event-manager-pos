package com.rjosefdev.eventos_api.inscricoes;

public record ResultadoCriacaoInscricao(
    InscricaoResponse inscricao,
    boolean criada
) {
}
