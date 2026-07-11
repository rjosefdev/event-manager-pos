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

    public AutenticacaoController(CadastroParticipanteService cadastroParticipanteService) {
        this.cadastroParticipanteService = cadastroParticipanteService;
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
    
}
