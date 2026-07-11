package com.rjosefdev.eventos_api.inscricoes;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rjosefdev.eventos_api.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/eventos/{eventoId}/inscricoes")
@PreAuthorize("hasRole('ORGANIZADOR')")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER_JWT)
public class InscritosEventoController {

    private final InscritosEventoService inscritosEventoService;

    public InscritosEventoController(InscritosEventoService inscritosEventoService) {
        this.inscritosEventoService = inscritosEventoService;
    }

    @GetMapping
    public List<InscritoEventoResponse> listar(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String eventoId
    ) {
        return inscritosEventoService.listarDoEvento(jwt.getSubject(), eventoId);
    }
}
