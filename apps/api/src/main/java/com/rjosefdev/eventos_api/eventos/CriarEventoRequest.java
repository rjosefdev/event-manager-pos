package com.rjosefdev.eventos_api.eventos;

import java.time.Instant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarEventoRequest(
    @NotBlank(message = "Informe o título do evento.")
    @Size(max = 120, message = "O título deve ter no máximo 120 caracteres.")
    String titulo,

    @NotBlank(message = "Informe a descrição do evento.")
    @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres.")
    String descricao,

    @NotNull(message = "Informe quando o evento começa.")
    Instant iniciaEm,

    @NotNull(message = "Informe quando o evento termina.")
    Instant terminaEm,

    @NotBlank(message = "Informe o local ou link do evento.")
    @Size(max = 180, message = "O local deve ter no máximo 180 caracteres.")
    String local,

    boolean online,

    @NotBlank(message = "Informe a categoria do evento.")
    @Size(max = 80, message = "A categoria deve ter no máximo 80 caracteres.")
    String categoria,

    @Min(value = 1, message = "Informe pelo menos uma vaga.")
    int vagas,

    @Size(max = 500, message = "A URL da imagem deve ter no máximo 500 caracteres.")
    String imagemUrl
) {
}
