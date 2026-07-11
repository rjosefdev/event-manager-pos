package com.rjosefdev.eventos_api.autenticacao;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.rjosefdev.eventos_api.autenticacao.TokenAcessoService.TokenAcessoGerado;
import com.rjosefdev.eventos_api.usuarios.Usuario;
import com.rjosefdev.eventos_api.usuarios.UsuarioRepository;

@Service
public class LoginService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenAcessoService tokenAcessoService;

    public LoginService(
        UsuarioRepository usuarioRepository,
        PasswordEncoder passwordEncoder,
        TokenAcessoService tokenAcessoService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenAcessoService = tokenAcessoService;
    }

    public LoginResponse autenticar(LoginRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmail(emailNormalizado)
            .filter(encontrado -> encontrado.isAtivo())
            .filter(encontrado -> passwordEncoder.matches(request.senha(), encontrado.getSenhaHash()))
            .orElseThrow(CredenciaisInvalidasException::new);

        TokenAcessoGerado token = tokenAcessoService.gerar(usuario);
        return new LoginResponse(
            token.valor(),
            "Bearer",
            token.expiraEm(),
            UsuarioResumoResponse.de(usuario)
        );
    }
}
