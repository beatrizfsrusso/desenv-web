package com.fysikos.model;

public class Usuario {

    private int id;
    private String nome;
    private String email;
    /** transient: o Gson nao serializa, entao o hash nunca sai numa resposta da API. */
    private transient String senhaHash;
    private String cpf;
    private boolean administrador;
    /**
     * Exclusao de conta e logica, nao fisica: apagar a linha levaria junto o
     * historico de pedidos do cliente (que a loja precisa manter). Conta com
     * ativo = false nao loga e some das listagens.
     */
    private boolean ativo = true;

    public Usuario() {
    }

    public Usuario(int id, String nome, String email, String senhaHash, String cpf, boolean administrador, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.cpf = cpf;
        this.administrador = administrador;
        this.ativo = ativo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public boolean isAdministrador() {
        return administrador;
    }

    public void setAdministrador(boolean administrador) {
        this.administrador = administrador;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
