package com.fysikos.dao;

import com.fysikos.model.Carrinho;
import com.fysikos.model.ItemCarrinho;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso ao carrinho (tabelas carrinho e produto_carrinho).
 *
 * Os metodos que recebem uma Connection sao usados pelo CheckoutService, que
 * precisa de varias operacoes na mesma transacao; os demais abrem a propria
 * conexao.
 */
public class CarrinhoDAO {

    public Carrinho obterOuCriar(int usuarioId) {
        try (Connection conn = ConnectionFactory.getConnection()) {
            return obterOuCriar(conn, usuarioId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter carrinho", e);
        }
    }

    public Carrinho obterOuCriar(Connection conn, int usuarioId) throws SQLException {
        int carrinhoId = obterOuCriarId(conn, usuarioId);
        Carrinho carrinho = new Carrinho(carrinhoId, usuarioId);
        carrinho.setItens(carregarItens(conn, carrinhoId));
        return carrinho;
    }

    /**
     * Garante que o usuario tem um carrinho e devolve o id. O RETURNING evita
     * uma segunda consulta: o INSERT ja devolve a linha em conflito.
     */
    public int obterOuCriarId(Connection conn, int usuarioId) throws SQLException {
        String sql = "INSERT INTO carrinho (usuario_id) VALUES (?) " +
                "ON CONFLICT (usuario_id) DO UPDATE SET usuario_id = EXCLUDED.usuario_id " +
                "RETURNING id";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        throw new SQLException("Nao foi possivel obter o carrinho do usuario " + usuarioId);
    }

    private List<ItemCarrinho> carregarItens(Connection conn, int carrinhoId) throws SQLException {
        List<ItemCarrinho> itens = new ArrayList<>();
        String sql = "SELECT pc.produto_id, p.nome, pc.quantidade, p.preco " +
                "FROM produto_carrinho pc " +
                "JOIN produto p ON p.id = pc.produto_id " +
                "WHERE pc.carrinho_id = ? " +
                "ORDER BY p.nome";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, carrinhoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    itens.add(new ItemCarrinho(
                            rs.getInt("produto_id"),
                            rs.getString("nome"),
                            rs.getInt("quantidade"),
                            rs.getBigDecimal("preco")
                    ));
                }
            }
        }
        return itens;
    }

    /**
     * Soma a quantidade se o produto ja estiver no carrinho, senao insere.
     * A chave primaria (carrinho_id, produto_id) e o que permite o ON CONFLICT.
     */
    public void adicionarItem(int carrinhoId, int produtoId, int quantidade) {
        try (Connection conn = ConnectionFactory.getConnection()) {
            adicionarItem(conn, carrinhoId, produtoId, quantidade);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao adicionar item ao carrinho", e);
        }
    }

    public void adicionarItem(Connection conn, int carrinhoId, int produtoId, int quantidade)
            throws SQLException {
        String sql = "INSERT INTO produto_carrinho (carrinho_id, produto_id, quantidade) VALUES (?, ?, ?) " +
                "ON CONFLICT (carrinho_id, produto_id) " +
                "DO UPDATE SET quantidade = produto_carrinho.quantidade + EXCLUDED.quantidade";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, carrinhoId);
            stmt.setInt(2, produtoId);
            stmt.setInt(3, quantidade);
            stmt.executeUpdate();
        }
    }

    public boolean atualizarQuantidade(int carrinhoId, int produtoId, int quantidade) {
        try (Connection conn = ConnectionFactory.getConnection()) {
            return atualizarQuantidade(conn, carrinhoId, produtoId, quantidade);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar quantidade no carrinho", e);
        }
    }

    public boolean atualizarQuantidade(Connection conn, int carrinhoId, int produtoId, int quantidade)
            throws SQLException {
        String sql = "UPDATE produto_carrinho SET quantidade = ? WHERE carrinho_id = ? AND produto_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantidade);
            stmt.setInt(2, carrinhoId);
            stmt.setInt(3, produtoId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean removerItem(int carrinhoId, int produtoId) {
        try (Connection conn = ConnectionFactory.getConnection()) {
            return removerItem(conn, carrinhoId, produtoId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao remover item do carrinho", e);
        }
    }

    public boolean removerItem(Connection conn, int carrinhoId, int produtoId) throws SQLException {
        String sql = "DELETE FROM produto_carrinho WHERE carrinho_id = ? AND produto_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, carrinhoId);
            stmt.setInt(2, produtoId);
            return stmt.executeUpdate() > 0;
        }
    }

    public void limpar(int carrinhoId) {
        try (Connection conn = ConnectionFactory.getConnection()) {
            limpar(conn, carrinhoId);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao limpar o carrinho", e);
        }
    }

    public void limpar(Connection conn, int carrinhoId) throws SQLException {
        String sql = "DELETE FROM produto_carrinho WHERE carrinho_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, carrinhoId);
            stmt.executeUpdate();
        }
    }
}
