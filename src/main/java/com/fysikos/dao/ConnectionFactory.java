package com.fysikos.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Conexao JDBC com o PostgreSQL. Os valores podem ser sobrescritos por variaveis
 * de ambiente (DB_URL, DB_USER, DB_PASSWORD); sem elas valem os defaults abaixo,
 * que batem com o docker-compose.yml do repositorio do banco (ecommerce-data).
 *
 * A senha vem do POSTGRES_PASSWORD definido no .env daquele repositorio: aqui o
 * padrao e "postgres", mas quem tiver outra senha exporta DB_PASSWORD antes de
 * subir a API.
 */
public class ConnectionFactory {

    private static final String URL = valor("DB_URL", "jdbc:postgresql://localhost:5433/ecommerce");
    private static final String USER = valor("DB_USER", "postgres");
    private static final String PASSWORD = valor("DB_PASSWORD", "postgres");

    private static String valor(String variavel, String padrao) {
        String lido = System.getenv(variavel);
        return (lido == null || lido.isBlank()) ? padrao : lido;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
