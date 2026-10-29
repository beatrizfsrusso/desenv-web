package com.fysikos.dao;

import com.fysikos.model.Pagamento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * Acesso a tabela pagamento.
 *
 * Nao ha integracao com gateway: o CheckoutService grava o pagamento ja como
 * APROVADO so para fechar o fluxo do trabalho.
 */
public class PagamentoDAO {

    public Pagamento inserir(Connection conn, Pagamento pagamento) throws SQLException {
        String sql = "INSERT INTO pagamento (pedido_id, metodo, status, valor, data_pagamento) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, pagamento.getPedidoId());
            stmt.setString(2, pagamento.getMetodo());
            stmt.setString(3, pagamento.getStatus());
            stmt.setBigDecimal(4, pagamento.getValor());
            stmt.setTimestamp(5, Timestamp.valueOf(pagamento.getDataPagamento()));
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    pagamento.setId(rs.getInt(1));
                }
            }
        }
        return pagamento;
    }

    public Pagamento obterPorPedido(int pedidoId) {
        try (Connection conn = ConnectionFactory.getConnection()) {
            return obterPorPedido(conn, pedidoId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter pagamento do pedido", e);
        }
    }

    public Pagamento obterPorPedido(Connection conn, int pedidoId) throws SQLException {
        String sql = "SELECT id, pedido_id, metodo, status, valor, data_pagamento " +
                "FROM pagamento WHERE pedido_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pedidoId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Pagamento(
                        rs.getInt("id"),
                        rs.getInt("pedido_id"),
                        rs.getString("metodo"),
                        rs.getString("status"),
                        rs.getBigDecimal("valor"),
                        rs.getTimestamp("data_pagamento").toLocalDateTime()
                );
            }
        }
    }
}
