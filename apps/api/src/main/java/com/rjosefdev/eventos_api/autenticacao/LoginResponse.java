package com.rjosefdev.eventos_api.autenticacao;

import java.time.Instant;

public record LoginResponse(
    String tokenAcesso,
    String tipoToken,
    Instant expiraEm,
    UsuarioResumoResponse usuario
) {
}
