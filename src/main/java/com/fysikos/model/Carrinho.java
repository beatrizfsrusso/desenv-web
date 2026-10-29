package com.fysikos.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Carrinho de um usuario (tabela carrinho). Cada usuario tem no maximo um,
 * garantido pelo UNIQUE em carrinho.usuario_id.
 */
public class Carrinho {

    private int id;
    private int usuarioId;
    private List<ItemCarrinho> itens = new ArrayList<>();

    public Carrinho() {
    }

    public Carrinho(int id, int usuarioId) {
        this.id = id;
        this.usuarioId = usuarioId;
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

    public List<ItemCarrinho> getItens() {
        return itens;
    }

    public void setItens(List<ItemCarrinho> itens) {
        this.itens = itens;
    }

    /** Soma dos subtotais. Calculado, nao vem do banco. */
    public BigDecimal getTotal() {
        return itens.stream()
                .map(ItemCarrinho::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
