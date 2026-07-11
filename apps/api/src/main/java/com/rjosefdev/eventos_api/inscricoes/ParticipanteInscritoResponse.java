package com.rjosefdev.eventos_api.inscricoes;

import com.rjosefdev.eventos_api.usuarios.Usuario;

public record ParticipanteInscritoResponse(
    String id,
    String nome,
    String email
) {
    public static ParticipanteInscritoResponse de(Usuario usuario) {
        return new ParticipanteInscritoResponse(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail()
        );
    }
}
