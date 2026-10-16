package com.fysikos.dao;

import com.fysikos.model.ImagemProduto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ImagemProdutoDAO {

    public List<ImagemProduto> obterPorProduto(int produtoId) {
        List<ImagemProduto> imagens = new ArrayList<>();
        String sql = "SELECT id, produto_id, caminho, principal FROM imagem_produto WHERE produto_id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, produtoId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    imagens.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter imagens do produto", e);
        }

        return imagens;
    }

    public ImagemProduto inserir(ImagemProduto imagem) {
        String sql = "INSERT INTO imagem_produto (produto_id, caminho, principal) VALUES (?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, imagem.getProdutoId());
            stmt.setString(2, imagem.getCaminho());
            stmt.setBoolean(3, imagem.isPrincipal());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    imagem.setId(rs.getInt(1));
                }
            }
            return imagem;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir imagem do produto", e);
        }
    }

    public boolean remover(int id) {
        String sql = "DELETE FROM imagem_produto WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao remover imagem do produto", e);
        }
    }

    private ImagemProduto mapear(ResultSet rs) throws SQLException {
        return new ImagemProduto(
                rs.getInt("id"),
                rs.getInt("produto_id"),
                rs.getString("caminho"),
                rs.getBoolean("principal")
        );
    }
}
