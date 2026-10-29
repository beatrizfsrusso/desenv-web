package com.fysikos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de pagamento do pedido (tabela pagamento).
 *
 * ATENCAO: nao existe integracao com gateway de pagamento. O registro e criado
 * com status APROVADO direto pelo CheckoutService para fechar o fluxo do
 * trabalho -- nenhum dado de cartao passa por aqui.
 */
public class Pagamento {

    private int id;
    private int pedidoId;
    private String metodo;
    private String status;
    private BigDecimal valor;
    private LocalDateTime dataPagamento;

    public Pagamento() {
    }

    public Pagamento(int id, int pedidoId, String metodo, String status, BigDecimal valor,
                     LocalDateTime dataPagamento) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.metodo = metodo;
        this.status = status;
        this.valor = valor;
        this.dataPagamento = dataPagamento;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(int pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getMetodo() {
        return metodo;
    }

    public void setMetodo(String metodo) {
        this.metodo = metodo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDateTime getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDateTime dataPagamento) {
        this.dataPagamento = dataPagamento;
    }
}
