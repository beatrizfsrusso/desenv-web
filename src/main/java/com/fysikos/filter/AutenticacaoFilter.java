package com.fysikos.filter;

import com.fysikos.controller.Resposta;
import com.fysikos.util.Sessao;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Porteiro da API. Roda depois do CorsFilter (a ordem esta no web.xml), entao
 * as respostas 401/403 daqui ja saem com os cabecalhos de CORS.
 *
 * Regras:
 *  - catalogo (GET de produtos, categorias, avaliacoes, imagens) e cadastro e
 *    login sao publicos;
 *  - o resto exige sessao;
 *  - escrita em produto, categoria, cupom e imagem exige administrador.
 *
 * A flag de administrador vem da propria sessao, gravada no login, para nao
 * consultar o banco a cada requisicao.
 */
public class AutenticacaoFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String caminho = req.getRequestURI().substring(req.getContextPath().length());
        String metodo = req.getMethod().toUpperCase();

        if (isPublica(caminho, metodo)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession sessao = req.getSession(false);
        Object usuarioId = sessao == null ? null : sessao.getAttribute(Sessao.ATRIBUTO_USUARIO_ID);

        if (usuarioId == null) {
            Resposta.erro(resp, HttpServletResponse.SC_UNAUTHORIZED, "Faca login para continuar");
            return;
        }

        if (exigeAdministrador(caminho, metodo)
                && !Boolean.TRUE.equals(sessao.getAttribute(Sessao.ATRIBUTO_ADMINISTRADOR))) {
            Resposta.erro(resp, HttpServletResponse.SC_FORBIDDEN, "Acesso restrito a administradores");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublica(String caminho, String metodo) {
        if ("OPTIONS".equals(metodo)) {
            return true;
        }

        if ("POST".equals(metodo)) {
            return caminho.equals("/api/usuarios")
                    || caminho.equals("/api/usuarios/login")
                    || caminho.equals("/api/usuarios/logout");
        }

        if ("GET".equals(metodo)) {
            return caminho.startsWith("/api/produtos")
                    || caminho.startsWith("/api/categorias")
                    || caminho.startsWith("/api/avaliacoes")
                    || caminho.startsWith("/api/imagens");
        }

        return false;
    }

    private boolean exigeAdministrador(String caminho, String metodo) {
        if ("GET".equals(metodo)) {
            return false;
        }
        return caminho.startsWith("/api/produtos")
                || caminho.startsWith("/api/categorias")
                || caminho.startsWith("/api/cupons")
                || caminho.startsWith("/api/imagens");
    }
}
