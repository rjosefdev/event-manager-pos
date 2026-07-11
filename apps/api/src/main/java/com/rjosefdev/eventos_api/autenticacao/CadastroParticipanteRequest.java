package com.rjosefdev.eventos_api.autenticacao;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroParticipanteRequest(
    @NotBlank(message = "é obrigatório")
    @Size(max = 120, message = "deve ter no máximo 120 caracteres")
    String nome,

    @NotBlank(message = "é obrigatório")
    @Email(message = "deve ser um e-mail válido")
    @Size(max = 254, message = "deve ter no máximo 254 caracteres")
    String email,

    @SenhaValida
    String senha
) {
}
