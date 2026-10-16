package com.fysikos.dao;

import com.fysikos.model.Avaliacao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AvaliacaoDAO {

    public List<Avaliacao> obterPorProduto(int produtoId) {
        List<Avaliacao> avaliacoes = new ArrayList<>();
        String sql = "SELECT id, usuario_id, produto_id, descricao, comentario, data, ativo " +
                "FROM avaliacao WHERE produto_id = ? AND ativo = TRUE";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, produtoId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    avaliacoes.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter avaliacoes do produto", e);
        }

        return avaliacoes;
    }

    public Avaliacao inserir(Avaliacao avaliacao) {
        String sql = "INSERT INTO avaliacao (usuario_id, produto_id, descricao, comentario, data, ativo) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, avaliacao.getUsuarioId());
            stmt.setInt(2, avaliacao.getProdutoId());
            stmt.setString(3, avaliacao.getDescricao());
            stmt.setString(4, avaliacao.getComentario());
            stmt.setDate(5, Date.valueOf(avaliacao.getData()));
            stmt.setBoolean(6, avaliacao.isAtivo());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    avaliacao.setId(rs.getInt(1));
                }
            }
            return avaliacao;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir avaliacao", e);
        }
    }

    public boolean remover(int id) {
        String sql = "UPDATE avaliacao SET ativo = FALSE WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao remover avaliacao", e);
        }
    }

    private Avaliacao mapear(ResultSet rs) throws SQLException {
        return new Avaliacao(
                rs.getInt("id"),
                rs.getInt("usuario_id"),
                rs.getInt("produto_id"),
                rs.getString("descricao"),
                rs.getString("comentario"),
                rs.getDate("data").toLocalDate(),
                rs.getBoolean("ativo")
        );
    }
}
