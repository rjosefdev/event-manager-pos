package com.rjosefdev.eventos_api.autenticacao;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/autenticacao")
public class AutenticacaoController {

    private final CadastroParticipanteService cadastroParticipanteService;
    private final LoginService loginService;

    public AutenticacaoController(
        CadastroParticipanteService cadastroParticipanteService,
        LoginService loginService
    ) {
        this.cadastroParticipanteService = cadastroParticipanteService;
        this.loginService = loginService;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<ParticipanteResponse> cadastrar(
        @Valid @RequestBody CadastroParticipanteRequest request
    ) {
        ParticipanteResponse participante = cadastroParticipanteService.cadastrar(request);
        return ResponseEntity
            .created(URI.create("/usuarios/" + participante.id()))
            .body(participante);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(loginService.autenticar(request));
    }
    
}
