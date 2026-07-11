package com.rjosefdev.eventos_api.autenticacao;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SenhaValidador implements ConstraintValidator<SenhaValida, String> {
    @Override
    public boolean isValid(String senha, ConstraintValidatorContext context) {
        return senha != null
            && senha.length() >= 8
            && senha.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
