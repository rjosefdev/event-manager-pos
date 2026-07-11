package com.rjosefdev.eventos_api.autenticacao;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ FIELD, PARAMETER, RECORD_COMPONENT, ANNOTATION_TYPE })
@Retention(RUNTIME)
@Constraint(validatedBy = SenhaValidador.class)
public @interface SenhaValida {
    String message() default "deve ter entre 8 caracteres e 72 bytes em UTF-8";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
