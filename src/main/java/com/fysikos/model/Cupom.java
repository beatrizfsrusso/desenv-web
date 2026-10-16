package com.fysikos.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Cupom {

    private int id;
    private String codigo;
    private String tipoDesconto;
    private BigDecimal valor;
    private LocalDate validade;
    private boolean ativo;

    public Cupom() {
    }

    public Cupom(int id, String codigo, String tipoDesconto, BigDecimal valor, LocalDate validade, boolean ativo) {
        this.id = id;
        this.codigo = codigo;
        this.tipoDesconto = tipoDesconto;
        this.valor = valor;
        this.validade = validade;
        this.ativo = ativo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getTipoDesconto() {
        return tipoDesconto;
    }

    public void setTipoDesconto(String tipoDesconto) {
        this.tipoDesconto = tipoDesconto;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDate getValidade() {
        return validade;
    }

    public void setValidade(LocalDate validade) {
        this.validade = validade;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
