package com.rjosefdev.eventos_api.config;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;

import com.rjosefdev.eventos_api.autenticacao.EmailJaCadastradoException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EmailJaCadastradoException.class)
    ProblemDetail emailJaCadastrado(
        EmailJaCadastradoException exception,
        ServletWebRequest request
    ) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problema.setType(URI.create("https://event-manager.local/problemas/email-ja-cadastrado"));
        problema.setTitle("E-mail já cadastrado");
        problema.setInstance(URI.create(request.getRequest().getRequestURI()));
        problema.setProperty("codigo", "EMAIL_JA_CADASTRADO");
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException exception, ServletWebRequest request) {
        Map<String, String> erros = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(
            erro -> erros.putIfAbsent(erro.getField(), erro.getDefaultMessage())
        );

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Revise os campos informados e tente novamente."
        );
        problema.setType(URI.create("https://event-manager.local/problemas/dados-invalidos"));
        problema.setTitle("Dados inválidos");
        problema.setInstance(URI.create(request.getRequest().getRequestURI()));
        problema.setProperty("codigo", "DADOS_INVALIDOS");
        problema.setProperty("erros", erros);
        return problema;
    }
}
