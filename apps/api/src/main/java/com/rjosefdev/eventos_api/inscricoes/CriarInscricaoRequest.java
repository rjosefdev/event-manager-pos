package com.rjosefdev.eventos_api.inscricoes;

import jakarta.validation.constraints.NotBlank;

public record CriarInscricaoRequest(
    @NotBlank(message = "Informe o Evento da Inscrição.")
    String eventoId
) {
}
