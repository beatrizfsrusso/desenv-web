package com.fysikos.dao;

import com.fysikos.model.Cupom;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CupomDAO {

    public List<Cupom> obterTodos() {
        List<Cupom> cupons = new ArrayList<>();
        String sql = "SELECT id, codigo, tipo_desconto, valor, validade, ativo FROM cupom";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                cupons.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter cupons", e);
        }

        return cupons;
    }

    public Cupom obterPorCodigo(String codigo) {
        String sql = "SELECT id, codigo, tipo_desconto, valor, validade, ativo FROM cupom WHERE codigo = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, codigo);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter cupom por codigo", e);
        }
    }

    public Cupom obterPorId(int id) {
        String sql = "SELECT id, codigo, tipo_desconto, valor, validade, ativo FROM cupom WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao obter cupom", e);
        }
    }

    /** Variante usada dentro da transacao de checkout, que precisa da mesma conexao. */
    public Cupom obterPorCodigo(Connection conn, String codigo) throws SQLException {
        String sql = "SELECT id, codigo, tipo_desconto, valor, validade, ativo FROM cupom WHERE codigo = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, codigo);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Um usuario so pode usar o mesmo cupom uma vez. A PK composta de uso_cupom
     * ja impediria a duplicata, mas checar antes permite devolver um erro claro
     * em vez de estourar uma violacao de constraint.
     */
    public boolean jaUsado(Connection conn, int cupomId, int usuarioId) throws SQLException {
        String sql = "SELECT 1 FROM uso_cupom WHERE id_cupom = ? AND usuario_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cupomId);
            stmt.setInt(2, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void registrarUso(Connection conn, int cupomId, int usuarioId, int pedidoId) throws SQLException {
        String sql = "INSERT INTO uso_cupom (id_cupom, usuario_id, pedido_id, data_uso) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, cupomId);
            stmt.setInt(2, usuarioId);
            stmt.setInt(3, pedidoId);
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
        }
    }

    public Cupom inserir(Cupom cupom) {
        String sql = "INSERT INTO cupom (codigo, tipo_desconto, valor, validade, ativo) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, cupom.getCodigo());
            stmt.setString(2, cupom.getTipoDesconto());
            stmt.setBigDecimal(3, cupom.getValor());
            stmt.setDate(4, Date.valueOf(cupom.getValidade()));
            stmt.setBoolean(5, cupom.isAtivo());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    cupom.setId(rs.getInt(1));
                }
            }
            return cupom;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir cupom", e);
        }
    }

    public boolean atualizar(Cupom cupom) {
        String sql = "UPDATE cupom SET tipo_desconto = ?, valor = ?, validade = ?, ativo = ? WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cupom.getTipoDesconto());
            stmt.setBigDecimal(2, cupom.getValor());
            stmt.setDate(3, Date.valueOf(cupom.getValidade()));
            stmt.setBoolean(4, cupom.isAtivo());
            stmt.setInt(5, cupom.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar cupom", e);
        }
    }

    public boolean remover(int id) {
        String sql = "DELETE FROM cupom WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao remover cupom", e);
        }
    }

    private Cupom mapear(ResultSet rs) throws SQLException {
        return new Cupom(
                rs.getInt("id"),
                rs.getString("codigo"),
                rs.getString("tipo_desconto"),
                rs.getBigDecimal("valor"),
                rs.getDate("validade").toLocalDate(),
                rs.getBoolean("ativo")
        );
    }
}
