package com.rjosefdev.eventos_api.autenticacao;

import com.rjosefdev.eventos_api.usuarios.Perfil;
import com.rjosefdev.eventos_api.usuarios.Usuario;

public record ParticipanteResponse(String id, String nome, String email, Perfil perfil) {
    public static ParticipanteResponse de(Usuario usuario) {
        return new ParticipanteResponse(
            usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil()
        );
    }
}
