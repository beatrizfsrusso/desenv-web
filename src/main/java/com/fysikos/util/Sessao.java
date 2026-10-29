package com.fysikos.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Acesso aos dados guardados na sessao pelo login. Os nomes dos atributos
 * ficam aqui para o filtro e os servlets nao saírem de sincronia.
 */
public final class Sessao {

    public static final String ATRIBUTO_USUARIO_ID = "usuarioId";
    public static final String ATRIBUTO_ADMINISTRADOR = "administrador";

    private Sessao() {
    }

    /** Id do usuario logado, ou null se nao houver sessao. */
    public static Integer usuarioId(HttpServletRequest req) {
        HttpSession sessao = req.getSession(false);
        if (sessao == null) {
            return null;
        }
        return (Integer) sessao.getAttribute(ATRIBUTO_USUARIO_ID);
    }

    public static boolean administrador(HttpServletRequest req) {
        HttpSession sessao = req.getSession(false);
        return sessao != null && Boolean.TRUE.equals(sessao.getAttribute(ATRIBUTO_ADMINISTRADOR));
    }
}
