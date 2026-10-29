package com.fysikos.dao;

import com.fysikos.model.Produto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ProdutoDAO {

    /**
     * A nota media e a contagem de avaliacoes vem da view produto_nota_media,
     * que o schema do banco define. O LEFT JOIN garante que produto sem
     * avaliacao nenhuma ainda apareca (com media null e contagem 0).
     */
    private static final String SELECT_BASE =
            "SELECT p.id, p.categoria_id, p.nome, p.descricao, p.preco, p.quantidade_estoque, p.ativo, " +
            "v.media_nota, v.total_avaliacoes " +
            "FROM produto p LEFT JOIN produto_nota_media v ON v.produto_id = p.id";

    public List<Produto> obterTodos() {
        List<Produto> produtos = new ArrayList<>();
        String sql = SELECT_BASE;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                produtos.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter produtos", e);
        }

        return produtos;
    }

    public List<Produto> obterPorCategoria(int categoriaId) {
        List<Produto> produtos = new ArrayList<>();
        String sql = SELECT_BASE + " WHERE p.categoria_id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, categoriaId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    produtos.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter produtos por categoria", e);
        }

        return produtos;
    }

    public List<Produto> buscarPorNome(String termo) {
        List<Produto> produtos = new ArrayList<>();
        String sql = SELECT_BASE + " WHERE p.nome ILIKE ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + termo + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    produtos.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar produtos por nome", e);
        }

        return produtos;
    }

    public Produto obterPorId(int id) {
        String sql = SELECT_BASE + " WHERE p.id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter produto", e);
        }
    }

    /** Variante usada dentro da transacao de checkout, que precisa da mesma conexao. */
    public Produto obterPorId(Connection conn, int id) throws SQLException {
        String sql = SELECT_BASE + " WHERE p.id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Da baixa no estoque somente se houver saldo suficiente. A condicao no
     * WHERE faz a checagem e o decremento na mesma instrucao, entao duas
     * compras simultaneas nao conseguem levar o estoque a negativo.
     *
     * @return false se o estoque era insuficiente (nada foi alterado)
     */
    public boolean baixarEstoque(Connection conn, int produtoId, int quantidade) throws SQLException {
        String sql = "UPDATE produto SET quantidade_estoque = quantidade_estoque - ? " +
                "WHERE id = ? AND quantidade_estoque >= ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quantidade);
            stmt.setInt(2, produtoId);
            stmt.setInt(3, quantidade);
            return stmt.executeUpdate() > 0;
        }
    }

    public Produto inserir(Produto produto) {
        String sql = "INSERT INTO produto (categoria_id, nome, descricao, preco, quantidade_estoque, ativo) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, produto.getCategoriaId());
            stmt.setString(2, produto.getNome());
            stmt.setString(3, produto.getDescricao());
            stmt.setBigDecimal(4, produto.getPreco());
            stmt.setInt(5, produto.getQuantidadeEstoque());
            stmt.setBoolean(6, produto.isAtivo());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    produto.setId(rs.getInt(1));
                }
            }
            return produto;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir produto", e);
        }
    }

    public boolean atualizar(Produto produto) {
        String sql = "UPDATE produto SET categoria_id = ?, nome = ?, descricao = ?, preco = ?, " +
                "quantidade_estoque = ?, ativo = ? WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, produto.getCategoriaId());
            stmt.setString(2, produto.getNome());
            stmt.setString(3, produto.getDescricao());
            stmt.setBigDecimal(4, produto.getPreco());
            stmt.setInt(5, produto.getQuantidadeEstoque());
            stmt.setBoolean(6, produto.isAtivo());
            stmt.setInt(7, produto.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar produto", e);
        }
    }

    public boolean remover(int id) {
        String sql = "DELETE FROM produto WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao remover produto", e);
        }
    }

    private Produto mapear(ResultSet rs) throws SQLException {
        return new Produto(
                rs.getInt("id"),
                rs.getInt("categoria_id"),
                rs.getString("nome"),
                rs.getString("descricao"),
                rs.getBigDecimal("preco"),
                rs.getInt("quantidade_estoque"),
                rs.getBoolean("ativo"),
                // getBigDecimal devolve null quando a view nao tem linha para o produto
                rs.getBigDecimal("media_nota"),
                rs.getInt("total_avaliacoes")
        );
    }
}
