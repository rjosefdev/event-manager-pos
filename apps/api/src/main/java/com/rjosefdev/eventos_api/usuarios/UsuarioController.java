package com.rjosefdev.eventos_api.usuarios;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rjosefdev.eventos_api.autenticacao.UsuarioResumoResponse;
import com.rjosefdev.eventos_api.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/usuarios")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER_JWT)
public class UsuarioController {

    private final UsuarioAtualService usuarioAtualService;

    public UsuarioController(UsuarioAtualService usuarioAtualService) {
        this.usuarioAtualService = usuarioAtualService;
    }

    @GetMapping("/atual")
    public UsuarioResumoResponse atual(@AuthenticationPrincipal Jwt jwt) {
        return usuarioAtualService.buscarPorSub(jwt.getSubject());
    }
}
