package com.rjosefdev.eventos_api.config;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class ErroSegurancaHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ErroSegurancaHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) throws IOException {
        escrever(response, problema(
            HttpStatus.UNAUTHORIZED,
            "Não autenticado",
            "Informe um token de acesso válido.",
            "NAO_AUTENTICADO",
            request
        ));
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        escrever(response, problema(
            HttpStatus.FORBIDDEN,
            "Acesso negado",
            "Você não tem permissão para acessar este recurso.",
            "ACESSO_NEGADO",
            request
        ));
    }

    private ProblemDetail problema(
        HttpStatus status,
        String title,
        String detail,
        String codigo,
        HttpServletRequest request
    ) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detail);
        problema.setType(URI.create("https://event-manager.local/problemas/" + codigo.toLowerCase()));
        problema.setTitle(title);
        problema.setInstance(URI.create(request.getRequestURI()));
        problema.setProperty("codigo", codigo);
        return problema;
    }

    private void escrever(HttpServletResponse response, ProblemDetail problema) throws IOException {
        response.setStatus(problema.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problema);
    }
}
