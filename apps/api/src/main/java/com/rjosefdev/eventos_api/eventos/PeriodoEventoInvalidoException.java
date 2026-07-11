package com.rjosefdev.eventos_api.eventos;

public class PeriodoEventoInvalidoException extends RuntimeException {
    public PeriodoEventoInvalidoException() {
        super("A data de término deve ser posterior à data de início.");
    }
}
