package com.rjosefdev.eventos_api.usuarios;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import com.rjosefdev.eventos_api.autenticacao.UsuarioResumoResponse;

@Service
public class UsuarioAtualService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioAtualService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public UsuarioResumoResponse buscarPorSub(String sub) {
        return usuarioRepository.findById(sub)
            .map(UsuarioResumoResponse::de)
            .orElseThrow(() -> new BadCredentialsException("Usuário do token não encontrado."));
    }
}
