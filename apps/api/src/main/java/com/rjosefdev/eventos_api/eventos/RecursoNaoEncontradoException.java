package com.rjosefdev.eventos_api.eventos;

public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException() {
        super("Recurso não encontrado.");
    }
}
