package com.rjosefdev.eventos_api.admin;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.rjosefdev.eventos_api.autenticacao.EmailJaCadastradoException;
import com.rjosefdev.eventos_api.usuarios.Perfil;
import com.rjosefdev.eventos_api.usuarios.Usuario;
import com.rjosefdev.eventos_api.usuarios.UsuarioRepository;

@Service
public class AdminService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AdminService(
        UsuarioRepository usuarioRepository,
        PasswordEncoder passwordEncoder,
        Clock clock
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public UsuarioAdminResponse cadastrarOrganizador(CadastroOrganizadorRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase(Locale.ROOT);
        Instant agora = clock.instant();
        Usuario organizador = new Usuario(
            request.nome().trim(),
            emailNormalizado,
            passwordEncoder.encode(request.senha()),
            Perfil.ORGANIZADOR,
            true,
            agora
        );

        try {
            return UsuarioAdminResponse.de(usuarioRepository.save(organizador));
        } catch (DuplicateKeyException exception) {
            throw new EmailJaCadastradoException();
        }
    }

    public List<UsuarioAdminResponse> listarUsuarios() {
        return usuarioRepository.findAll().stream()
            .map(UsuarioAdminResponse::de)
            .toList();
    }
}
