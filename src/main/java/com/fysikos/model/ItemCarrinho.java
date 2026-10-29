package com.fysikos.model;

import java.math.BigDecimal;

/**
 * Uma linha do carrinho (tabela produto_carrinho). O nome e o preco vem do
 * produto no momento da consulta, para o front nao precisar de outra chamada.
 */
public class ItemCarrinho {

    private int produtoId;
    private String nome;
    private int quantidade;
    private BigDecimal precoUnitario;

    public ItemCarrinho() {
    }

    public ItemCarrinho(int produtoId, String nome, int quantidade, BigDecimal precoUnitario) {
        this.produtoId = produtoId;
        this.nome = nome;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    public int getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(int produtoId) {
        this.produtoId = produtoId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    /** Calculado, nao vem do banco. */
    public BigDecimal getSubtotal() {
        if (precoUnitario == null) {
            return BigDecimal.ZERO;
        }
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
