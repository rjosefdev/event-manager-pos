package com.rjosefdev.eventos_api.autenticacao;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.rjosefdev.eventos_api.usuarios.Usuario;

@Service
public class TokenAcessoService {

    public static final String AUDIENCIA = "event-manager-api";

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final String issuer;
    private final long expiraEmSegundos;

    public TokenAcessoService(
        JwtEncoder jwtEncoder,
        Clock clock,
        @Value("${app.security.jwt.issuer}") String issuer,
        @Value("${app.security.jwt.expira-em-segundos}") long expiraEmSegundos
    ) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.issuer = issuer;
        this.expiraEmSegundos = expiraEmSegundos;
    }

    public TokenAcessoGerado gerar(Usuario usuario) {
        Instant emitidoEm = clock.instant();
        Instant expiraEm = emitidoEm.plusSeconds(expiraEmSegundos);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .audience(List.of(AUDIENCIA))
            .subject(usuario.getId())
            .issuedAt(emitidoEm)
            .expiresAt(expiraEm)
            .claim("perfil", usuario.getPerfil().name())
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenAcessoGerado(token, expiraEm);
    }

    public record TokenAcessoGerado(String valor, Instant expiraEm) {
    }
}
