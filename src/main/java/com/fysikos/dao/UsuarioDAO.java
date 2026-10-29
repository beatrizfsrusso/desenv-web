package com.fysikos.dao;

import com.fysikos.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    public List<Usuario> obterTodos() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT id, nome, email, senha_hash, cpf, administrador, ativo FROM usuario WHERE ativo = TRUE";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                usuarios.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter usuarios", e);
        }

        return usuarios;
    }

    public Usuario obterPorId(int id) {
        String sql = "SELECT id, nome, email, senha_hash, cpf, administrador, ativo FROM usuario WHERE id = ? AND ativo = TRUE";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter usuario", e);
        }
    }

    /**
     * Busca pelo email, sem filtrar por {@code ativo} — de proposito. O login
     * precisa encontrar a conta desativada para recusar o acesso, e o cadastro
     * precisa encontrar a linha que ainda ocupa o email (a coluna e UNIQUE, entao
     * deixar cadastrar de novo estouraria no banco).
     */
    public Usuario obterPorEmail(String email) {
        String sql = "SELECT id, nome, email, senha_hash, cpf, administrador, ativo FROM usuario WHERE email = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter usuario por email", e);
        }
    }

    public Usuario inserir(Usuario usuario) {
        String sql = "INSERT INTO usuario (nome, email, senha_hash, cpf, administrador) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, usuario.getSenhaHash());
            stmt.setString(4, usuario.getCpf());
            stmt.setBoolean(5, usuario.isAdministrador());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    usuario.setId(rs.getInt(1));
                }
            }
            return usuario;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir usuario", e);
        }
    }

    public boolean atualizar(Usuario usuario) {
        String sql = "UPDATE usuario SET nome = ?, email = ?, cpf = ? WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, usuario.getCpf());
            stmt.setInt(4, usuario.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar usuario", e);
        }
    }

    /**
     * Troca a senha. Fica separado de {@link #atualizar} de proposito: aquele
     * metodo cuida dos dados cadastrais e nunca toca em senha_hash.
     */
    public boolean atualizarSenha(int id, String senhaHash) {
        String sql = "UPDATE usuario SET senha_hash = ? WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, senhaHash);
            stmt.setInt(2, id);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar a senha do usuario", e);
        }
    }

    /**
     * Desativa a conta em vez de apagar a linha. Apagar de verdade esbarraria nas
     * foreign keys de carrinho, pedido, avaliacao e uso_cupom (nenhuma delas
     * cascateia) e levaria junto o historico de pedidos do cliente.
     *
     * @return false se o usuario nao existe ou ja estava desativado.
     */
    public boolean remover(int id) {
        String sql = "UPDATE usuario SET ativo = FALSE WHERE id = ? AND ativo = TRUE";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao desativar usuario", e);
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("email"),
                rs.getString("senha_hash"),
                rs.getString("cpf"),
                rs.getBoolean("administrador"),
                rs.getBoolean("ativo")
        );
    }
}
