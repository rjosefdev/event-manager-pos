package com.rjosefdev.eventos_api.eventos;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rjosefdev.eventos_api.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/eventos")
@PreAuthorize("hasRole('ORGANIZADOR')")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER_JWT)
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @PostMapping
    public ResponseEntity<EventoResponse> criar(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CriarEventoRequest request
    ) {
        EventoResponse evento = eventoService.criar(jwt.getSubject(), request);
        return ResponseEntity
            .created(URI.create("/eventos/" + evento.id()))
            .body(evento);
    }

    @GetMapping
    public List<EventoResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return eventoService.listarDoOrganizador(jwt.getSubject());
    }

    @GetMapping("/{id}")
    public EventoResponse buscar(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String id
    ) {
        return eventoService.buscarDoOrganizador(jwt.getSubject(), id);
    }

    @PutMapping("/{id}")
    public EventoResponse atualizar(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String id,
        @Valid @RequestBody AtualizarEventoRequest request
    ) {
        return eventoService.atualizar(jwt.getSubject(), id, request);
    }

    @DeleteMapping("/{id}")
    public EventoResponse cancelar(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable String id
    ) {
        return eventoService.cancelar(jwt.getSubject(), id);
    }
}
