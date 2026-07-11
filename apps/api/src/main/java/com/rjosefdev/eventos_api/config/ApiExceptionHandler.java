package com.rjosefdev.eventos_api.config;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;

import com.rjosefdev.eventos_api.autenticacao.CredenciaisInvalidasException;
import com.rjosefdev.eventos_api.autenticacao.EmailJaCadastradoException;
import com.rjosefdev.eventos_api.eventos.EventoFinalizadoException;
import com.rjosefdev.eventos_api.eventos.EventoImagemInvalidaException;
import com.rjosefdev.eventos_api.eventos.PeriodoEventoInvalidoException;
import com.rjosefdev.eventos_api.eventos.RecursoNaoEncontradoException;
import com.rjosefdev.eventos_api.inscricoes.InscricaoNaoPermitidaException;

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

    @ExceptionHandler(CredenciaisInvalidasException.class)
    ProblemDetail credenciaisInvalidas(
        CredenciaisInvalidasException exception,
        ServletWebRequest request
    ) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
        problema.setType(URI.create("https://event-manager.local/problemas/credenciais-invalidas"));
        problema.setTitle("Credenciais inválidas");
        problema.setInstance(URI.create(request.getRequest().getRequestURI()));
        problema.setProperty("codigo", "CREDENCIAIS_INVALIDAS");
        return problema;
    }

    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail sessaoInvalida(
        BadCredentialsException exception,
        ServletWebRequest request
    ) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Informe um token de acesso válido."
        );
        problema.setType(URI.create("https://event-manager.local/problemas/nao-autenticado"));
        problema.setTitle("Não autenticado");
        problema.setInstance(URI.create(request.getRequest().getRequestURI()));
        problema.setProperty("codigo", "NAO_AUTENTICADO");
        return problema;
    }

    @ExceptionHandler(PeriodoEventoInvalidoException.class)
    ProblemDetail periodoEventoInvalido(
        PeriodoEventoInvalidoException exception,
        ServletWebRequest request
    ) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problema.setType(URI.create("https://event-manager.local/problemas/periodo-evento-invalido"));
        problema.setTitle("Período do evento inválido");
        problema.setInstance(URI.create(request.getRequest().getRequestURI()));
        problema.setProperty("codigo", "PERIODO_EVENTO_INVALIDO");
        return problema;
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail recursoNaoEncontrado(
        RecursoNaoEncontradoException exception,
        ServletWebRequest request
    ) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problema.setType(URI.create("https://event-manager.local/problemas/recurso-nao-encontrado"));
        problema.setTitle("Recurso não encontrado");
        problema.setInstance(URI.create(request.getRequest().getRequestURI()));
        problema.setProperty("codigo", "RECURSO_NAO_ENCONTRADO");
        return problema;
    }

    @ExceptionHandler(EventoFinalizadoException.class)
    ProblemDetail eventoFinalizado(
        EventoFinalizadoException exception,
        ServletWebRequest request
    ) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problema.setType(URI.create("https://event-manager.local/problemas/evento-finalizado"));
        problema.setTitle("Evento finalizado");
        problema.setInstance(URI.create(request.getRequest().getRequestURI()));
        problema.setProperty("codigo", "EVENTO_FINALIZADO");
        return problema;
    }

    @ExceptionHandler(EventoImagemInvalidaException.class)
    ProblemDetail eventoImagemInvalida(
        EventoImagemInvalidaException exception,
        ServletWebRequest request
    ) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problema.setType(URI.create("https://event-manager.local/problemas/evento-imagem-invalida"));
        problema.setTitle("Imagem do evento inválida");
        problema.setInstance(URI.create(request.getRequest().getRequestURI()));
        problema.setProperty("codigo", "EVENTO_IMAGEM_INVALIDA");
        return problema;
    }

    @ExceptionHandler(InscricaoNaoPermitidaException.class)
    ProblemDetail inscricaoNaoPermitida(
        InscricaoNaoPermitidaException exception,
        ServletWebRequest request
    ) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problema.setType(URI.create("https://event-manager.local/problemas/inscricao-nao-permitida"));
        problema.setTitle("Inscrição não permitida");
        problema.setInstance(URI.create(request.getRequest().getRequestURI()));
        problema.setProperty("codigo", "INSCRICAO_NAO_PERMITIDA");
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
