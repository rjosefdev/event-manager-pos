package com.rjosefdev.eventos_api.catalogo;

import java.util.Locale;

import com.rjosefdev.eventos_api.eventos.Evento;

public record CatalogoFiltro(
    String busca,
    String categoria,
    String ordem
) {
    public static CatalogoFiltro semFiltros() {
        return new CatalogoFiltro(null, null, null);
    }

    public boolean corresponde(Evento evento) {
        return correspondeCategoria(evento) && correspondeBusca(evento);
    }

    public boolean ordemDecrescente() {
        String valor = normalizar(ordem);
        return "desc".equals(valor) || "decrescente".equals(valor);
    }

    private boolean correspondeCategoria(Evento evento) {
        String categoriaNormalizada = normalizar(categoria);
        if (categoriaNormalizada == null) {
            return true;
        }
        return categoriaNormalizada.equals(normalizar(evento.getCategoria()));
    }

    private boolean correspondeBusca(Evento evento) {
        String buscaNormalizada = normalizar(busca);
        if (buscaNormalizada == null) {
            return true;
        }
        return contem(evento.getTitulo(), buscaNormalizada)
            || contem(evento.getDescricao(), buscaNormalizada);
    }

    private boolean contem(String valor, String buscaNormalizada) {
        String valorNormalizado = normalizar(valor);
        return valorNormalizado != null && valorNormalizado.contains(buscaNormalizada);
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim().toLowerCase(Locale.ROOT);
    }
}
