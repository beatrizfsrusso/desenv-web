package com.fysikos.model;

import java.time.LocalDate;

public class Avaliacao {

    private int id;
    private int usuarioId;
    private int produtoId;
    private String descricao;
    private String comentario;
    private LocalDate data;
    private boolean ativo;

    public Avaliacao() {
    }

    public Avaliacao(int id, int usuarioId, int produtoId, String descricao, String comentario,
                      LocalDate data, boolean ativo) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.produtoId = produtoId;
        this.descricao = descricao;
        this.comentario = comentario;
        this.data = data;
        this.ativo = ativo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public int getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(int produtoId) {
        this.produtoId = produtoId;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
