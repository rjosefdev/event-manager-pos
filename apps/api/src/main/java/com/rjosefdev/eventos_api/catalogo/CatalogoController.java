package com.rjosefdev.eventos_api.catalogo;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rjosefdev.eventos_api.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/catalogo/eventos")
@PreAuthorize("hasRole('PARTICIPANTE')")
@SecurityRequirement(name = OpenApiConfig.ESQUEMA_BEARER_JWT)
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping
    public List<CatalogoEventoResponse> listar(
        @RequestParam(required = false) String busca,
        @RequestParam(required = false) String categoria,
        @RequestParam(required = false) String ordem
    ) {
        return catalogoService.listar(new CatalogoFiltro(busca, categoria, ordem));
    }

    @GetMapping("/{id}")
    public CatalogoEventoResponse buscarDetalhes(@PathVariable String id) {
        return catalogoService.buscarDetalhes(id);
    }
}
