package com.rjosefdev.eventos_api.usuarios;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;
    private String nome;
    private String email;
    private String senhaHash;
    private Perfil perfil;
    private boolean ativo;
    private Instant criadoEm;
    private Instant atualizadoEm;

    public Usuario() {
    }

    public Usuario(String nome, String email, String senhaHash, Perfil perfil, boolean ativo, Instant criadoEm) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = criadoEm;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public Perfil getPerfil() { return perfil; }
    public boolean isAtivo() { return ativo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }

    public void setId(String id) { this.id = id; }
}
