package com.fysikos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido fechado (tabela pedido). Nasce no checkout e ja entra com status
 * PAGO e um pagamento APROVADO simulado.
 */
public class Pedido {

    private int id;
    private int usuarioId;
    private int enderecoId;
    private LocalDateTime dataHora;
    private String status;
    private BigDecimal valorFrete;
    private BigDecimal desconto;
    private BigDecimal valorTotal;
    private List<ItemPedido> itens = new ArrayList<>();
    private Pagamento pagamento;

    public Pedido() {
    }

    public Pedido(int id, int usuarioId, int enderecoId, LocalDateTime dataHora, String status,
                  BigDecimal valorFrete, BigDecimal desconto, BigDecimal valorTotal) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.enderecoId = enderecoId;
        this.dataHora = dataHora;
        this.status = status;
        this.valorFrete = valorFrete;
        this.desconto = desconto;
        this.valorTotal = valorTotal;
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

    public int getEnderecoId() {
        return enderecoId;
    }

    public void setEnderecoId(int enderecoId) {
        this.enderecoId = enderecoId;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getValorFrete() {
        return valorFrete;
    }

    public void setValorFrete(BigDecimal valorFrete) {
        this.valorFrete = valorFrete;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public void setDesconto(BigDecimal desconto) {
        this.desconto = desconto;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }

    public void setItens(List<ItemPedido> itens) {
        this.itens = itens;
    }

    public Pagamento getPagamento() {
        return pagamento;
    }

    public void setPagamento(Pagamento pagamento) {
        this.pagamento = pagamento;
    }
}
