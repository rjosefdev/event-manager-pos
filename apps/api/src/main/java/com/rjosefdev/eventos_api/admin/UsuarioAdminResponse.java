package com.rjosefdev.eventos_api.admin;

import java.time.Instant;

import com.rjosefdev.eventos_api.usuarios.Perfil;
import com.rjosefdev.eventos_api.usuarios.Usuario;

public record UsuarioAdminResponse(
    String id,
    String nome,
    String email,
    Perfil perfil,
    boolean ativo,
    Instant criadoEm,
    Instant atualizadoEm
) {
    public static UsuarioAdminResponse de(Usuario usuario) {
        return new UsuarioAdminResponse(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getPerfil(),
            usuario.isAtivo(),
            usuario.getCriadoEm(),
            usuario.getAtualizadoEm()
        );
    }
}
