package com.fysikos.dao;

import com.fysikos.model.Endereco;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EnderecoDAO {

    public List<Endereco> obterPorUsuario(int usuarioId) {
        List<Endereco> enderecos = new ArrayList<>();
        String sql = "SELECT id, usuario_id, cep, logradouro, numero, complemento, bairro, cidade, estado, principal " +
                "FROM endereco WHERE usuario_id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, usuarioId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    enderecos.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter enderecos do usuario", e);
        }

        return enderecos;
    }

    public Endereco obterPorId(int id) {
        String sql = "SELECT id, usuario_id, cep, logradouro, numero, complemento, bairro, cidade, estado, principal " +
                "FROM endereco WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter endereco", e);
        }
    }

    /** Variante usada dentro da transacao de checkout, que precisa da mesma conexao. */
    public Endereco obterPorId(Connection conn, int id) throws SQLException {
        String sql = "SELECT id, usuario_id, cep, logradouro, numero, complemento, bairro, cidade, estado, principal " +
                "FROM endereco WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Endereco inserir(Endereco endereco) {
        String sql = "INSERT INTO endereco (usuario_id, cep, logradouro, numero, complemento, bairro, cidade, " +
                "estado, principal) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, endereco.getUsuarioId());
            stmt.setString(2, endereco.getCep());
            stmt.setString(3, endereco.getLogradouro());
            stmt.setString(4, endereco.getNumero());
            stmt.setString(5, endereco.getComplemento());
            stmt.setString(6, endereco.getBairro());
            stmt.setString(7, endereco.getCidade());
            stmt.setString(8, endereco.getEstado());
            stmt.setBoolean(9, endereco.isPrincipal());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    endereco.setId(rs.getInt(1));
                }
            }
            return endereco;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir endereco", e);
        }
    }

    public boolean atualizar(Endereco endereco) {
        String sql = "UPDATE endereco SET cep = ?, logradouro = ?, numero = ?, complemento = ?, bairro = ?, " +
                "cidade = ?, estado = ?, principal = ? WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, endereco.getCep());
            stmt.setString(2, endereco.getLogradouro());
            stmt.setString(3, endereco.getNumero());
            stmt.setString(4, endereco.getComplemento());
            stmt.setString(5, endereco.getBairro());
            stmt.setString(6, endereco.getCidade());
            stmt.setString(7, endereco.getEstado());
            stmt.setBoolean(8, endereco.isPrincipal());
            stmt.setInt(9, endereco.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar endereco", e);
        }
    }

    public boolean remover(int id) {
        String sql = "DELETE FROM endereco WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao remover endereco", e);
        }
    }

    private Endereco mapear(ResultSet rs) throws SQLException {
        return new Endereco(
                rs.getInt("id"),
                rs.getInt("usuario_id"),
                rs.getString("cep"),
                rs.getString("logradouro"),
                rs.getString("numero"),
                rs.getString("complemento"),
                rs.getString("bairro"),
                rs.getString("cidade"),
                rs.getString("estado"),
                rs.getBoolean("principal")
        );
    }
}
