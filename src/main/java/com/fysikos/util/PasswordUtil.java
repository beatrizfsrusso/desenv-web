package com.fysikos.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Hash de senha com BCrypt (salt aleatorio embutido no proprio hash).
 * O hash tem 60 caracteres, cabe em usuario.senha_hash VARCHAR(255).
 */
public class PasswordUtil {

    private static final int CUSTO = 12;

    public static String hash(String senha) {
        return BCrypt.hashpw(senha, BCrypt.gensalt(CUSTO));
    }

    /**
     * Compara a senha em texto puro com o hash guardado. O BCrypt gera um salt
     * novo a cada chamada, entao a comparacao precisa ser feita por ele.
     */
    public static boolean verifica(String senha, String hashArmazenado) {
        if (senha == null || hashArmazenado == null || hashArmazenado.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(senha, hashArmazenado);
        } catch (IllegalArgumentException e) {
            // Hash em formato inesperado (ex.: registros antigos em SHA-256).
            return false;
        }
    }
}
