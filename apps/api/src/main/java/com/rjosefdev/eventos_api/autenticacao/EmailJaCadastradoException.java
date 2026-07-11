package com.rjosefdev.eventos_api.autenticacao;

public class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException() {
        super("Já existe um Usuário cadastrado com este e-mail.");
    }
}
