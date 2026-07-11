package com.rjosefdev.eventos_api.eventos;

public class EventoFinalizadoException extends RuntimeException {
    public EventoFinalizadoException() {
        super("Eventos finalizados não podem ser alterados ou cancelados.");
    }
}
