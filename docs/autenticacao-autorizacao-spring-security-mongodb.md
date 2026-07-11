# Autenticacao e autorizacao com Spring Security, Resource Server e MongoDB

Guia de implementacao para um fluxo simples de cadastro, login, JWT Bearer Token e autorizacao por perfil no projeto **Event Manager - Pos**.

Este guia considera o backend atual em `apps/api`, com Spring Boot `4.1.0`, Java 17, Web MVC e MongoDB.

## 1. Escopo da solucao

A solucao proposta tem estes objetivos:

- Salvar usuarios na collection `usuarios` do MongoDB.
- Salvar senha somente como hash BCrypt.
- Permitir cadastro e login por e-mail e senha.
- Retornar um JWT no login.
- Proteger endpoints com `Authorization: Bearer <token>`.
- Autorizar acoes por perfil:
  - `ORGANIZADOR`: cria e gerencia eventos.
  - `PARTICIPANTE`: consulta eventos e gerencia inscricoes.

Importante: `spring-boot-starter-security-oauth2-resource-server` faz a API validar tokens Bearer. Ele nao cria sozinho endpoints de cadastro, login ou emissao de JWT. Para um sistema simples, a propria API pode emitir tokens no `/autenticacao/login` e validar esses tokens como resource server. Em sistemas maiores, o ideal e separar isso em um provedor de identidade ou em um Authorization Server dedicado.

## 2. Dependencias

No Spring Boot `4.1.0`, as dependencias principais ficam assim:

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
```

Responsabilidade de cada uma:

| Dependencia | Para que serve |
|---|---|
| `spring-boot-starter-security` | Habilita a base do Spring Security: filtros, `SecurityFilterChain`, `AuthenticationManager`, autorizacao por URL/metodo, `PasswordEncoder`, protecoes HTTP etc. |
| `spring-boot-starter-security-oauth2-resource-server` | Habilita suporte para API protegida por OAuth2 Bearer Token, especialmente validacao de JWT recebido no header `Authorization`. |

Nota sobre nomes: no Spring Boot 4.1.0, `spring-boot-starter-security-oauth2-resource-server` e o artifactId recomendado. O artifactId antigo `spring-boot-starter-oauth2-resource-server` aparece como depreciado em favor do novo nome.

## 3. Arquitetura recomendada

Fluxo de cadastro:

```txt
POST /autenticacao/cadastro
  -> valida dados
  -> verifica e-mail duplicado
  -> gera BCrypt da senha
  -> salva Usuario no MongoDB
  -> retorna dados publicos do usuario
```

Fluxo de login:

```txt
POST /autenticacao/login
  -> AuthenticationManager valida e-mail/senha
  -> UserDetailsService carrega usuario do MongoDB
  -> PasswordEncoder compara senha informada com senhaHash
  -> ServicoTokenAutenticacao emite JWT
  -> retorna tokenAcesso
```

Fluxo de requisicao autenticado:

```txt
GET /eventos/me
Authorization: Bearer eyJ...
  -> Resource Server valida assinatura, exp, issuer e claims do JWT
  -> Spring Security monta Authentication
  -> regras de URL/metodo decidem se pode acessar
```

## 4. Modelo de usuario no MongoDB

Collection: `usuarios`

Documento exemplo:

```json
{
  "_id": { "$oid": "66a000000000000000000001" },
  "nome": "Organizador Demo",
  "email": "organizador.demo@email.com",
  "senhaHash": "$2a$10$hash_bcrypt_apenas_exemplo",
  "perfil": "ORGANIZADOR",
  "ativo": true,
  "criadoEm": { "$date": "2026-07-08T00:00:00.000Z" },
  "atualizadoEm": { "$date": "2026-07-08T00:00:00.000Z" }
}
```

Indice recomendado:

```js
db.usuarios.createIndex(
  { email: 1 },
  { unique: true, nome: "uk_usuarios_email" }
);
```

Enums:

```java
package com.rjosefdev.eventos_api.usuarios;

public enum Perfil {
    ORGANIZADOR,
    PARTICIPANTE
}
```

Entidade:

```java
package com.rjosefdev.eventos_api.usuarios;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;

    private String nome;

    @Indexed(unique = true)
    private String email;

    private String senhaHash;

    private Perfil perfil;

    private boolean ativo = true;

    private Instant criadoEm;

    private Instant atualizadoEm;

    // getters e setters
}
```

Repository:

```java
package com.rjosefdev.eventos_api.usuarios;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

## 5. Configuracoes

`application.properties`:

```properties
spring.config.import=optional:file:.env[.properties],optional:file:apps/api/.env[.properties]
spring.application.name=eventos-api
spring.mongodb.uri=${MONGODB_URI}

app.security.jwt.issuer=${JWT_ISSUER:eventos-api}
app.security.jwt.expires-in-seconds=${JWT_EXPIRES_IN_SECONDS:3600}
app.security.jwt.secret=${JWT_SECRET}
```

O MVP usa `HS256` com um `JWT_SECRET` de no minimo 32 bytes. A aplicacao deve falhar na inicializacao se o segredo estiver ausente ou for curto demais. O segredo nao deve ser versionado.

Exemplo em `.env` local:

```properties
MONGODB_URI=mongodb://localhost:27017/event_manager_pos
JWT_ISSUER=eventos-api
JWT_EXPIRES_IN_SECONDS=3600
JWT_SECRET=troque-por-um-segredo-longo-com-pelo-menos-32-bytes
```

## 6. SecurityConfig

Responsabilidades:

- Liberar somente `/autenticacao/cadastro` e `/autenticacao/login`.
- Exigir autenticacao no restante da API.
- Configurar stateless session.
- Liberar CORS para qualquer origem com `allowCredentials: false` e permitir preflight `OPTIONS` sem JWT.
- Desabilitar CSRF, pois a API nao usa autenticacao por cookie.
- Ativar JWT Resource Server.
- Expor `PasswordEncoder`.
- Mapear roles do JWT para authorities `ROLE_ORGANIZADOR` e `ROLE_PARTICIPANTE`.

```java
package com.rjosefdev.eventos_api.autenticacao.security;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/autenticacao/cadastro",
                    "/autenticacao/login"
                ).permitAll()
                .requestMatchers("/eventos/meus/**").hasRole("ORGANIZADOR")
                .requestMatchers("/inscricoes/minhas/**").hasRole("PARTICIPANTE")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(8);
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    JwtEncoder jwtEncoder(@Value("${app.security.jwt.secret}") String secret) {
        SecretKey secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(
        @Value("${app.security.jwt.secret}") String secret,
        @Value("${app.security.jwt.issuer}") String issuer
    ) {
        SecretKey secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();

        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuer);
        decoder.setJwtValidator(validator);

        return decoder;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        String perfil = jwt.getClaimAsString("perfil");
        if (perfil == null || perfil.isBlank()) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil));
    }
}
```

Observacao: se o projeto reclamar de imports relacionados a `NimbusJwtEncoder`, `NimbusJwtDecoder` ou `com.nimbusds`, verifique o `mvn dependency:tree`. Para JWT, o Spring Security precisa dos modulos de resource server e JOSE; o starter de resource server normalmente resolve isso de forma transitiva.

## 7. UserDetailsService usando MongoDB

O Spring Security usa `UserDetailsService` para carregar usuario, senha e authorities durante a autenticacao por e-mail/senha.

```java
package com.rjosefdev.eventos_api.autenticacao.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.rjosefdev.eventos_api.usuarios.UsuarioRepository;

@Service
public class MongoUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public MongoUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        var usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));

        return org.springframework.security.core.userdetails.User
            .withUsername(usuario.getEmail())
            .password(usuario.getSenhaHash())
            .roles(usuario.getPerfil().name())
            .disabled(!usuario.isAtivo())
            .build();
    }
}
```

## 8. DTOs de autenticacao

```java
package com.rjosefdev.eventos_api.autenticacao.dto;

import com.rjosefdev.eventos_api.usuarios.Perfil;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequisicaoCadastro(
    @NotBlank String nome,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8) String senha
) {
}
```

A senha deve ter no minimo 8 caracteres e no maximo 72 bytes em UTF-8. Ela nao sofre `trim` nem normalizacao e nao exige combinacoes artificiais de tipos de caractere.

```java
package com.rjosefdev.eventos_api.autenticacao.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequisicaoLogin(
    @Email @NotBlank String email,
    @NotBlank String senha
) {
}
```

```java
package com.rjosefdev.eventos_api.autenticacao.dto;

import com.rjosefdev.eventos_api.usuarios.Perfil;

public record RespostaAutenticacao(
    String tokenAcesso,
    String tipoToken,
    Instant expiraEm,
    ResumoUsuario usuario
) {
    public record ResumoUsuario(
        String id,
        String nome,
        String email,
        Perfil perfil
    ) {
    }
}
```

```java
package com.rjosefdev.eventos_api.autenticacao.dto;

import com.rjosefdev.eventos_api.usuarios.Perfil;

public record RespostaCadastro(
    String id,
    String nome,
    String email,
    Perfil perfil
) {
}
```

## 9. Servico para emitir JWT

Claims recomendadas:

| Claim | Conteudo |
|---|---|
| `iss` | Emissor configurado em `app.security.jwt.issuer`. |
| `sub` | Id do usuario. |
| `perfil` | `ORGANIZADOR` ou `PARTICIPANTE`. |
| `aud` | Audiencia fixa `event-manager-api`. |
| `iat` | Data de emissao. |
| `exp` | Data de expiracao. |

```java
package com.rjosefdev.eventos_api.autenticacao.security;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.rjosefdev.eventos_api.usuarios.Usuario;

@Service
public class ServicoTokenAutenticacao {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long expiresInSeconds;

    public ServicoTokenAutenticacao(
        JwtEncoder jwtEncoder,
        @Value("${app.security.jwt.issuer}") String issuer,
        @Value("${app.security.jwt.expires-in-seconds}") long expiresInSeconds
    ) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expiresInSeconds = expiresInSeconds;
    }

    public TokenAcessoGerado gerarTokenAcesso(Usuario usuario) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expiresInSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(usuario.getId())
            .audience(List.of("event-manager-api"))
            .claim("perfil", usuario.getPerfil().name())
            .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenAcessoGerado(valor, expiresAt);
    }

    public record TokenAcessoGerado(String valor, Instant expiraEm) {
    }
}
```

## 10. ServicoAutenticacao

```java
package com.rjosefdev.eventos_api.autenticacao;

import java.time.Instant;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.rjosefdev.eventos_api.autenticacao.dto.RespostaAutenticacao;
import com.rjosefdev.eventos_api.autenticacao.dto.RespostaCadastro;
import com.rjosefdev.eventos_api.autenticacao.dto.RequisicaoLogin;
import com.rjosefdev.eventos_api.autenticacao.dto.RequisicaoCadastro;
import com.rjosefdev.eventos_api.autenticacao.security.ServicoTokenAutenticacao;
import com.rjosefdev.eventos_api.usuarios.Usuario;
import com.rjosefdev.eventos_api.usuarios.UsuarioRepository;
import com.rjosefdev.eventos_api.usuarios.Perfil;

@Service
public class ServicoAutenticacao {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final ServicoTokenAutenticacao servicoTokenAutenticacao;

    public ServicoAutenticacao(
        UsuarioRepository usuarioRepository,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        ServicoTokenAutenticacao servicoTokenAutenticacao
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.servicoTokenAutenticacao = servicoTokenAutenticacao;
    }

    public RespostaCadastro cadastrar(RequisicaoCadastro requisicao) {
        if (usuarioRepository.existsByEmail(requisicao.email())) {
            throw new IllegalArgumentException("E-mail ja cadastrado");
        }

        Instant now = Instant.now();

        Usuario usuario = new Usuario();
        usuario.setNome(requisicao.nome());
        usuario.setEmail(requisicao.email().toLowerCase());
        usuario.setSenhaHash(passwordEncoder.encode(requisicao.senha()));
        usuario.setPerfil(Perfil.PARTICIPANTE);
        usuario.setAtivo(true);
        usuario.setCriadoEm(now);
        usuario.setAtualizadoEm(now);

        Usuario salvo = usuarioRepository.save(usuario);

        return new RespostaCadastro(
            salvo.getId(),
            salvo.getNome(),
            salvo.getEmail(),
            salvo.getPerfil()
        );
    }

    public RespostaAutenticacao login(RequisicaoLogin requisicao) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(requisicao.email().toLowerCase(), requisicao.senha())
        );

        Usuario usuario = usuarioRepository.findByEmail(requisicao.email().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Credenciais invalidas"));

        var token = servicoTokenAutenticacao.gerarTokenAcesso(usuario);
        return toRespostaAutenticacao(usuario, token);
    }

    private RespostaAutenticacao toRespostaAutenticacao(
        Usuario usuario,
        ServicoTokenAutenticacao.TokenAcessoGerado token
    ) {
        return new RespostaAutenticacao(
            token.valor(),
            "Bearer",
            token.expiraEm(),
            new RespostaAutenticacao.ResumoUsuario(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil()
            )
        );
    }
}
```

## 11. ControladorAutenticacao

```java
package com.rjosefdev.eventos_api.autenticacao;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.rjosefdev.eventos_api.autenticacao.dto.RespostaAutenticacao;
import com.rjosefdev.eventos_api.autenticacao.dto.RequisicaoLogin;
import com.rjosefdev.eventos_api.autenticacao.dto.RequisicaoCadastro;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/autenticacao")
public class ControladorAutenticacao {

    private final ServicoAutenticacao servicoAutenticacao;

    public ControladorAutenticacao(ServicoAutenticacao servicoAutenticacao) {
        this.servicoAutenticacao = servicoAutenticacao;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public RespostaCadastro cadastrar(@Valid @RequestBody RequisicaoCadastro requisicao) {
        return servicoAutenticacao.cadastrar(requisicao);
    }

    @PostMapping("/login")
    public RespostaAutenticacao login(@Valid @RequestBody RequisicaoLogin requisicao) {
        return servicoAutenticacao.login(requisicao);
    }
}
```

## 12. Autorizacao por endpoint e por metodo

Voce pode autorizar por URL na `SecurityFilterChain`:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/eventos/meus/**").hasRole("ORGANIZADOR")
    .requestMatchers("/inscricoes/minhas/**").hasRole("PARTICIPANTE")
    .anyRequest().authenticated()
)
```

E tambem por metodo, usando `@EnableMethodSecurity`:

```java
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasRole('ORGANIZADOR')")
public RespostaEvento createEvento(RequisicaoCriarEvento requisicao) {
    // ...
}

@PreAuthorize("hasRole('PARTICIPANTE')")
public RespostaInscricao registerForEvento(String eventoId) {
    // ...
}
```

Regra pratica:

- Use regras por URL para proteger grupos grandes de rotas.
- Use `@PreAuthorize` para proteger operacoes de negocio especificas.
- Mesmo com perfil correta, valide dono do recurso no service. Exemplo: um organizador so pode editar evento cujo `organizadorId` seja o proprio usuario autenticado.

## 13. Como acessar o usuario autenticado

Como o JWT usa `sub` com o id do usuario, voce pode ler o usuario atual assim:

```java
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@GetMapping("/usuarios/atual")
public UsuarioMeResponse me(@AuthenticationPrincipal Jwt jwt) {
    String userId = jwt.getSubject();
    String perfil = jwt.getClaimAsString("perfil");

    // Busca o Usuario pelo userId e retorna id, nome, email e perfil.
}
```

Para regras de dominio, normalmente e melhor transformar isso em um componente `CurrentUsuario`:

```java
package com.rjosefdev.eventos_api.autenticacao.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentUsuario {

    public String id() {
        JwtAuthenticationToken authentication =
            (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        return authentication.getToken().getSubject();
    }

    public String perfil() {
        JwtAuthenticationToken authentication =
            (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        return authentication.getToken().getClaimAsString("perfil");
    }
}
```

## 14. Contratos HTTP sugeridos

Cadastro:

```http
POST /autenticacao/cadastro
Content-Type: application/json

{
  "nome": "Participante Demo",
  "email": "participante.demo@email.com",
  "senha": "senha-forte-123"
}
```

Resposta:

```json
{
  "id": "66a000000000000000000001",
  "nome": "Participante Demo",
  "email": "participante.demo@email.com",
  "perfil": "PARTICIPANTE"
}
```

O cadastro não emite token. O Participante deve chamar o endpoint de login separadamente.

Login:

```http
POST /autenticacao/login
Content-Type: application/json

{
  "email": "organizador.demo@email.com",
  "senha": "senha-forte-123"
}
```

Resposta:

```json
{
  "tokenAcesso": "eyJ...",
  "tipoToken": "Bearer",
  "expiraEm": "2026-07-11T18:30:00Z",
  "usuario": {
    "id": "66a000000000000000000001",
    "nome": "Organizador Demo",
    "email": "organizador.demo@email.com",
    "perfil": "ORGANIZADOR"
  }
}
```

Request autenticado:

```http
GET /eventos/meus
Authorization: Bearer eyJ...
```

## 15. Checklist de implementacao

As respostas de erro usam `ProblemDetail` conforme RFC 9457. Alem dos campos padronizados, a API inclui `codigo`; erros de validacao incluem tambem `erros`, organizado por campo.

```json
{
  "type": "about:blank",
  "title": "Credenciais invalidas",
  "status": 401,
  "detail": "E-mail ou senha invalidos.",
  "instance": "/autenticacao/login",
  "codigo": "CREDENCIAIS_INVALIDAS"
}
```

1. Confirmar dependencias no `pom.xml`.
2. Criar pacote `usuarios` com `Usuario`, `Perfil` e `UsuarioRepository`.
3. Criar pacote `autenticacao.dto` com DTOs de cadastro, login e resposta.
4. Criar `MongoUserDetailsService`.
5. Criar `SecurityConfig` com `SecurityFilterChain`, `PasswordEncoder`, `JwtEncoder` e `JwtDecoder`.
6. Criar `ServicoTokenAutenticacao`.
7. Criar `ServicoAutenticacao`.
8. Criar `ControladorAutenticacao`.
9. Adicionar propriedades `app.security.jwt.*`.
10. Criar tratamento global para erros de validacao, e-mail duplicado e credenciais invalidas.
11. Adicionar testes de:
    - cadastro com sucesso;
    - cadastro com e-mail repetido;
    - login com sucesso;
    - login com senha invalida;
    - rota protegida sem token retorna 401;
    - rota de `ORGANIZADOR` com `PARTICIPANTE` retorna 403;
    - rota de `PARTICIPANTE` com `ORGANIZADOR` retorna 403.

## 16. Cuidados importantes

- Nunca salve senha pura.
- Nunca retorne `senhaHash` em resposta HTTP.
- Normalize e-mail com `trim` e lowercase usando `Locale.ROOT` antes de salvar e autenticar.
- Use indice unico em `usuarios.email`.
- Defina expiracao curta para access token.
- O MVP nao usa refresh token. Quando o access token expirar apos 1 hora, o usuario deve fazer login novamente.
- CSRF pode ficar desabilitado em API stateless consumida via Bearer Token. Se a API usar cookie de sessao no futuro, reavalie essa configuracao.
- Nao confie apenas na perfil. Para acoes por propriedade, valide tambem o dono do recurso.
- Em producao, troque segredo HMAC local por chaves assimetricas ou provedor de identidade dedicado.

## 17. Fontes consultadas

- Spring Security - OAuth 2.0 Resource Server JWT: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
- Spring Security - Authorize HTTP Requests: https://docs.spring.io/spring-security/reference/servlet/autenticacaoorization/autenticacaoorize-http-requests.html
- Spring Security - PasswordEncoder: https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/password-encoder.html
- Spring Security - UserDetailsService: https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/usuario-details-service.html
- Maven Central - `spring-boot-starter-security-oauth2-resource-server`: https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter-security-oauth2-resource-server
- Maven Central - `spring-boot-starter-oauth2-resource-server`: https://central.sonatype.com/artifact/org.springframework.boot/spring-boot-starter-oauth2-resource-server
