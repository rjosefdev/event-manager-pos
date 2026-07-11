package com.rjosefdev.eventos_api.autenticacao;

import com.rjosefdev.eventos_api.usuarios.Perfil;
import com.rjosefdev.eventos_api.usuarios.Usuario;

public record UsuarioResumoResponse(
    String id,
    String nome,
    String email,
    Perfil perfil
) {
    public static UsuarioResumoResponse de(Usuario usuario) {
        return new UsuarioResumoResponse(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getPerfil()
        );
    }
}
