package com.rjosefdev.eventos_api.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import com.rjosefdev.eventos_api.config.SegurancaConfig;
import com.rjosefdev.eventos_api.usuarios.Perfil;
import com.rjosefdev.eventos_api.usuarios.Usuario;

class TokenAcessoServiceTest {

    private static final String SEGREDO = "12345678901234567890123456789012";
    private static final String ISSUER = "event-manager-api";

    private final SegurancaConfig segurancaConfig = new SegurancaConfig();

    @Test
    void geraJwtHs256ComClaimsAcordadosEExpiracaoDeUmaHora() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-11T15:00:00Z"), ZoneOffset.UTC);
        TokenAcessoService service = new TokenAcessoService(
            segurancaConfig.jwtEncoder(SEGREDO),
            clock,
            ISSUER,
            3600
        );
        JwtDecoder decoder = segurancaConfig.jwtDecoder(SEGREDO, ISSUER);

        TokenAcessoService.TokenAcessoGerado token = service.gerar(usuario(Perfil.PARTICIPANTE));
        Jwt jwt = decoder.decode(token.valor());

        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(ISSUER);
        assertThat(jwt.getAudience()).containsExactly(TokenAcessoService.AUDIENCIA);
        assertThat(jwt.getSubject()).isEqualTo("usuario-1");
        assertThat(jwt.getClaimAsString("perfil")).isEqualTo("PARTICIPANTE");
        assertThat(jwt.getIssuedAt()).isEqualTo(Instant.parse("2026-07-11T15:00:00Z"));
        assertThat(jwt.getExpiresAt()).isEqualTo(Instant.parse("2026-07-11T16:00:00Z"));
        assertThat(token.expiraEm()).isEqualTo(jwt.getExpiresAt());
        assertThat(jwt.getClaims().keySet())
            .containsExactlyInAnyOrder("iss", "aud", "sub", "iat", "exp", "perfil");
    }

    @Test
    void rejeitaTokenAdulteradoExpiradoOuComAudienciaIncorreta() {
        String tokenValido = gerarToken(Clock.fixed(Instant.parse("2026-07-11T15:00:00Z"), ZoneOffset.UTC));
        String tokenAdulterado = tokenValido.substring(0, tokenValido.length() - 1) + "x";
        String tokenExpirado = gerarToken(Clock.fixed(Instant.parse("2026-07-10T00:00:00Z"), ZoneOffset.UTC));
        String tokenComAudienciaIncorreta = gerarTokenComAudiencia("outra-api");
        JwtDecoder decoder = segurancaConfig.jwtDecoder(SEGREDO, ISSUER);

        assertThatThrownBy(() -> decoder.decode(tokenAdulterado)).hasMessageContaining("Invalid signature");
        assertThatThrownBy(() -> decoder.decode(tokenExpirado)).hasMessageContaining("expired");
        assertThatThrownBy(() -> decoder.decode(tokenComAudienciaIncorreta)).hasMessageContaining("audiência");
        assertThatThrownBy(() -> segurancaConfig.jwtDecoder(SEGREDO, "outro-issuer").decode(tokenValido))
            .hasMessageContaining("iss claim");
        assertThatThrownBy(() -> segurancaConfig.jwtDecoder("abcdefghijklmnopqrstuvxyz1234567", ISSUER).decode(tokenValido))
            .hasMessageContaining("Invalid signature");
    }

    @Test
    void segredoComMenosDeTrintaEDoisBytesImpedeConfiguracao() {
        assertThatThrownBy(() -> segurancaConfig.jwtEncoder("curto"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("JWT_SECRET deve ter no mínimo 32 bytes.");
    }

    private String gerarToken(Clock clock) {
        TokenAcessoService service = new TokenAcessoService(
            segurancaConfig.jwtEncoder(SEGREDO),
            clock,
            ISSUER,
            3600
        );
        return service.gerar(usuario(Perfil.PARTICIPANTE)).valor();
    }

    private String gerarTokenComAudiencia(String audiencia) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(ISSUER)
            .audience(List.of(audiencia))
            .subject("usuario-1")
            .issuedAt(Instant.parse("2026-07-11T15:00:00Z"))
            .expiresAt(Instant.parse("2026-07-11T16:00:00Z"))
            .claim("perfil", "PARTICIPANTE")
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return segurancaConfig.jwtEncoder(SEGREDO)
            .encode(JwtEncoderParameters.from(header, claims))
            .getTokenValue();
    }

    private Usuario usuario(Perfil perfil) {
        Usuario usuario = new Usuario(
            "Participante",
            "participante@exemplo.com",
            "hash",
            perfil,
            true,
            Instant.parse("2026-07-11T15:00:00Z")
        );
        usuario.setId("usuario-1");
        return usuario;
    }
}
