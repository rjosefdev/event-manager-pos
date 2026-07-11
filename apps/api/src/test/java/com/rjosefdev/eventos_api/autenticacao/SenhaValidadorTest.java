package com.rjosefdev.eventos_api.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class SenhaValidadorTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void aceitaEspacosESimbolosSemAlterarASenha() {
        assertThat(validar(" abc 123! ")).isEmpty();
    }

    @Test
    void rejeitaSenhaComMenosDeOitoCaracteres() {
        assertThat(validar("1234567")).hasSize(1);
    }

    @Test
    void rejeitaSenhaQueUltrapassaSetentaEDoisBytesUtf8() {
        assertThat(validar("á".repeat(37))).hasSize(1);
    }

    private Set<ConstraintViolation<Entrada>> validar(String senha) {
        return validator.validate(new Entrada(senha));
    }

    private record Entrada(@SenhaValida String senha) {
    }
}
