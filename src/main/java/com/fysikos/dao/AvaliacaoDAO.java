package com.fysikos.dao;

import com.fysikos.model.Avaliacao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Avaliacoes de produto.
 *
 * A tabela nao tem id: a chave e composta por (usuario_id, produto_id). Entao
 * nao existe "obter por id" — as buscas pontuais sao por essa dupla.
 */
public class AvaliacaoDAO {

    private static final String COLUNAS =
            "usuario_id, produto_id, nota, descricao, comentario, data, ativo";

    public List<Avaliacao> obterPorProduto(int produtoId) {
        List<Avaliacao> avaliacoes = new ArrayList<>();
        String sql = "SELECT " + COLUNAS + " FROM avaliacao WHERE produto_id = ? AND ativo = TRUE";

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

    public Avaliacao obterPorChave(int usuarioId, int produtoId) {
        String sql = "SELECT " + COLUNAS + " FROM avaliacao WHERE usuario_id = ? AND produto_id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, usuarioId);
            stmt.setInt(2, produtoId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter avaliacao", e);
        }
    }

    /**
     * Insere a avaliacao; se aquele usuario ja avaliou aquele produto, atualiza
     * a existente em vez de estourar a chave primaria. A avaliacao volta a ficar
     * ativa, para o caso de ter sido apagada antes.
     */
    public Avaliacao inserir(Avaliacao avaliacao) {
        String sql = "INSERT INTO avaliacao (" + COLUNAS + ") VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (usuario_id, produto_id) DO UPDATE SET " +
                "nota = EXCLUDED.nota, descricao = EXCLUDED.descricao, " +
                "comentario = EXCLUDED.comentario, data = EXCLUDED.data, ativo = TRUE";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, avaliacao.getUsuarioId());
            stmt.setInt(2, avaliacao.getProdutoId());
            stmt.setInt(3, avaliacao.getNota());
            stmt.setString(4, avaliacao.getDescricao());
            stmt.setString(5, avaliacao.getComentario());
            stmt.setDate(6, Date.valueOf(avaliacao.getData()));
            stmt.setBoolean(7, avaliacao.isAtivo());
            stmt.executeUpdate();

            return avaliacao;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir avaliacao", e);
        }
    }

    /** Soft delete: marca ativo = FALSE preservando o historico. */
    public boolean remover(int usuarioId, int produtoId) {
        String sql = "UPDATE avaliacao SET ativo = FALSE WHERE usuario_id = ? AND produto_id = ? AND ativo = TRUE";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, usuarioId);
            stmt.setInt(2, produtoId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao remover avaliacao", e);
        }
    }

    private Avaliacao mapear(ResultSet rs) throws SQLException {
        return new Avaliacao(
                rs.getInt("usuario_id"),
                rs.getInt("produto_id"),
                rs.getInt("nota"),
                rs.getString("descricao"),
                rs.getString("comentario"),
                rs.getDate("data").toLocalDate(),
                rs.getBoolean("ativo")
        );
    }
}
