package com.fysikos.model;

import java.time.LocalDate;

/**
 * Avaliacao de um produto por um usuario.
 *
 * Nao tem id proprio: a tabela usa chave primaria composta (usuario_id,
 * produto_id), ou seja, cada usuario avalia cada produto uma vez so.
 */
public class Avaliacao {

    private int usuarioId;
    private int produtoId;
    private int nota;
    private String descricao;
    private String comentario;
    private LocalDate data;
    private boolean ativo;

    public Avaliacao() {
    }

    public Avaliacao(int usuarioId, int produtoId, int nota, String descricao, String comentario,
                     LocalDate data, boolean ativo) {
        this.usuarioId = usuarioId;
        this.produtoId = produtoId;
        this.nota = nota;
        this.descricao = descricao;
        this.comentario = comentario;
        this.data = data;
        this.ativo = ativo;
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

    /** Nota de 1 a 5 (o CHECK do banco garante a faixa). */
    public int getNota() {
        return nota;
    }

    public void setNota(int nota) {
        this.nota = nota;
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
