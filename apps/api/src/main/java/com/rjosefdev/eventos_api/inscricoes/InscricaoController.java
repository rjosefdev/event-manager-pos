package com.rjosefdev.eventos_api.inscricoes;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rjosefdev.eventos_api.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/inscricoes")
@PreAuthorize("hasRole('PARTICIPANTE')")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER_JWT)
public class InscricaoController {

    private final InscricaoService inscricaoService;

    public InscricaoController(InscricaoService inscricaoService) {
        this.inscricaoService = inscricaoService;
    }

    @PostMapping
    public ResponseEntity<InscricaoResponse> criar(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CriarInscricaoRequest request
    ) {
        ResultadoCriacaoInscricao resultado = inscricaoService.criar(jwt.getSubject(), request);
        InscricaoResponse inscricao = resultado.inscricao();
        if (!resultado.criada()) {
            return ResponseEntity.ok(inscricao);
        }

        return ResponseEntity
            .created(URI.create("/inscricoes/" + inscricao.id()))
            .body(inscricao);
    }

    @GetMapping
    public List<InscricaoResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return inscricaoService.listar(jwt.getSubject());
    }

    @DeleteMapping("/{id}")
    public InscricaoResponse cancelar(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String id
    ) {
        return inscricaoService.cancelar(jwt.getSubject(), id);
    }

    @PatchMapping("/{id}/reativar")
    public InscricaoResponse reativar(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String id
    ) {
        return inscricaoService.reativar(jwt.getSubject(), id);
    }
}
