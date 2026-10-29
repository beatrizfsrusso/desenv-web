package com.fysikos.model;

import java.math.BigDecimal;

public class Produto {

    private int id;
    private int categoriaId;
    private String nome;
    private String descricao;
    private BigDecimal preco;
    private int quantidadeEstoque;
    private boolean ativo;

    /**
     * Vem da view produto_nota_media, nao de coluna da tabela: e o que alimenta
     * as estrelas e a contagem de avaliacoes na vitrine. notaMedia e null quando
     * o produto ainda nao tem nenhuma avaliacao ativa.
     */
    private BigDecimal notaMedia;
    private int totalAvaliacoes;

    public Produto() {
    }

    public Produto(int id, int categoriaId, String nome, String descricao, BigDecimal preco,
                   int quantidadeEstoque, boolean ativo, BigDecimal notaMedia, int totalAvaliacoes) {
        this.id = id;
        this.categoriaId = categoriaId;
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.quantidadeEstoque = quantidadeEstoque;
        this.ativo = ativo;
        this.notaMedia = notaMedia;
        this.totalAvaliacoes = totalAvaliacoes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(int categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public void setQuantidadeEstoque(int quantidadeEstoque) {
        this.quantidadeEstoque = quantidadeEstoque;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public BigDecimal getNotaMedia() {
        return notaMedia;
    }

    public void setNotaMedia(BigDecimal notaMedia) {
        this.notaMedia = notaMedia;
    }

    public int getTotalAvaliacoes() {
        return totalAvaliacoes;
    }

    public void setTotalAvaliacoes(int totalAvaliacoes) {
        this.totalAvaliacoes = totalAvaliacoes;
    }
}
