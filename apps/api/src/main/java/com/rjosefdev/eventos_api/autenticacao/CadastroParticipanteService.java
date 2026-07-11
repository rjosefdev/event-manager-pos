package com.rjosefdev.eventos_api.autenticacao;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.rjosefdev.eventos_api.usuarios.Perfil;
import com.rjosefdev.eventos_api.usuarios.Usuario;
import com.rjosefdev.eventos_api.usuarios.UsuarioRepository;

@Service
public class CadastroParticipanteService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public CadastroParticipanteService(
        UsuarioRepository usuarioRepository,
        PasswordEncoder passwordEncoder,
        Clock clock
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public ParticipanteResponse cadastrar(CadastroParticipanteRequest request) {
        String emailNormalizado = request.email().trim().toLowerCase(Locale.ROOT);
        Instant agora = clock.instant();
        Usuario participante = new Usuario(
            request.nome().trim(),
            emailNormalizado,
            passwordEncoder.encode(request.senha()),
            Perfil.PARTICIPANTE,
            true,
            agora
        );

        try {
            return ParticipanteResponse.de(usuarioRepository.save(participante));
        } catch (DuplicateKeyException exception) {
            throw new EmailJaCadastradoException();
        }
    }
}
