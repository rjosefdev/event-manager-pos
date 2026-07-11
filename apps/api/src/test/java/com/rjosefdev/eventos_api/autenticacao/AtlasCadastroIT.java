package com.rjosefdev.eventos_api.autenticacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.rjosefdev.eventos_api.usuarios.IndiceUsuariosConfig;
import com.rjosefdev.eventos_api.usuarios.Perfil;
import com.rjosefdev.eventos_api.usuarios.Usuario;
import com.rjosefdev.eventos_api.usuarios.UsuarioRepository;

@SpringBootTest
class AtlasCadastroIT {

    @Autowired private CadastroParticipanteService service;
    @Autowired private UsuarioRepository repository;
    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private PasswordEncoder encoder;

    @Test
    void persisteParticipanteRealEGaranteIndiceUnico() {
        String marcador = UUID.randomUUID().toString();
        String email = "atlas-" + marcador + "@example.test";
        String idCriado = null;

        try {
            ParticipanteResponse response = service.cadastrar(
                new CadastroParticipanteRequest("Participante Atlas " + marcador, "  " + email.toUpperCase() + "  ", "senha atlas 123")
            );
            idCriado = response.id();

            Usuario persistido = repository.findById(idCriado).orElseThrow();
            assertThat(persistido.getEmail()).isEqualTo(email);
            assertThat(persistido.getPerfil()).isEqualTo(Perfil.PARTICIPANTE);
            assertThat(encoder.matches("senha atlas 123", persistido.getSenhaHash())).isTrue();
            assertThat(mongoTemplate.indexOps(Usuario.class).getIndexInfo())
                .anySatisfy(index -> {
                    assertThat(index.getName()).isEqualTo(IndiceUsuariosConfig.NOME_INDICE_EMAIL);
                    assertThat(index.isUnique()).isTrue();
                });
        } finally {
            if (idCriado != null) {
                repository.deleteById(idCriado);
            }
        }
    }
}
