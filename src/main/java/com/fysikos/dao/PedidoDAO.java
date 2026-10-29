package com.fysikos.dao;

import com.fysikos.model.ItemPedido;
import com.fysikos.model.Pedido;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Acesso a pedido e produto_pedido. Os metodos que recebem Connection sao
 * usados pelo CheckoutService dentro da transacao de checkout.
 */
public class PedidoDAO {

    private static final String COLUNAS =
            "id, usuario_id, endereco_id, data_hora, status, valor_frete, desconto, valor_total";

    public Pedido inserir(Connection conn, Pedido pedido) throws SQLException {
        String sql = "INSERT INTO pedido (usuario_id, endereco_id, data_hora, status, valor_frete, " +
                "desconto, valor_total) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, pedido.getUsuarioId());
            stmt.setInt(2, pedido.getEnderecoId());
            stmt.setTimestamp(3, Timestamp.valueOf(pedido.getDataHora()));
            stmt.setString(4, pedido.getStatus());
            stmt.setBigDecimal(5, pedido.getValorFrete());
            stmt.setBigDecimal(6, pedido.getDesconto());
            stmt.setBigDecimal(7, pedido.getValorTotal());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    pedido.setId(rs.getInt(1));
                }
            }
        }
        return pedido;
    }

    public void inserirItens(Connection conn, int pedidoId, List<ItemPedido> itens) throws SQLException {
        String sql = "INSERT INTO produto_pedido (pedido_id, produto_id, quantidade, preco_unitario) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (ItemPedido item : itens) {
                stmt.setInt(1, pedidoId);
                stmt.setInt(2, item.getProdutoId());
                stmt.setInt(3, item.getQuantidade());
                stmt.setBigDecimal(4, item.getPrecoUnitario());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public List<Pedido> obterPorUsuario(int usuarioId) {
        String sql = "SELECT " + COLUNAS + " FROM pedido WHERE usuario_id = ? ORDER BY data_hora DESC";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Pedido> pedidos = new ArrayList<>();
                while (rs.next()) {
                    Pedido pedido = mapear(rs);
                    pedido.setItens(carregarItens(conn, pedido.getId()));
                    pedidos.add(pedido);
                }
                return pedidos;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter pedidos do usuario", e);
        }
    }

    public List<Pedido> obterTodos() {
        String sql = "SELECT " + COLUNAS + " FROM pedido ORDER BY data_hora DESC";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<Pedido> pedidos = new ArrayList<>();
            while (rs.next()) {
                Pedido pedido = mapear(rs);
                pedido.setItens(carregarItens(conn, pedido.getId()));
                pedidos.add(pedido);
            }
            return pedidos;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter pedidos", e);
        }
    }

    public Pedido obterPorId(int id) {
        String sql = "SELECT " + COLUNAS + " FROM pedido WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Pedido pedido = mapear(rs);
                pedido.setItens(carregarItens(conn, pedido.getId()));
                pedido.setPagamento(new PagamentoDAO().obterPorPedido(conn, pedido.getId()));
                return pedido;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter pedido", e);
        }
    }

    private List<ItemPedido> carregarItens(Connection conn, int pedidoId) throws SQLException {
        List<ItemPedido> itens = new ArrayList<>();
        String sql = "SELECT pp.produto_id, p.nome, pp.quantidade, pp.preco_unitario " +
                "FROM produto_pedido pp " +
                "JOIN produto p ON p.id = pp.produto_id " +
                "WHERE pp.pedido_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pedidoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    itens.add(new ItemPedido(
                            rs.getInt("produto_id"),
                            rs.getString("nome"),
                            rs.getInt("quantidade"),
                            rs.getBigDecimal("preco_unitario")
                    ));
                }
            }
        }
        return itens;
    }

    private Pedido mapear(ResultSet rs) throws SQLException {
        return new Pedido(
                rs.getInt("id"),
                rs.getInt("usuario_id"),
                rs.getInt("endereco_id"),
                rs.getTimestamp("data_hora").toLocalDateTime(),
                rs.getString("status"),
                rs.getBigDecimal("valor_frete"),
                rs.getBigDecimal("desconto"),
                rs.getBigDecimal("valor_total")
        );
    }
}
