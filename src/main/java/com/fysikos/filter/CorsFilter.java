package com.fysikos.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CORS para o front em React, que roda em outra porta.
 *
 * Como a API usa sessao por cookie, a origem NAO pode ser "*": o navegador
 * recusa "*" junto com credenciais. Por isso a origem da requisicao e ecoada
 * de volta apenas quando esta na lista de permitidas.
 *
 * A lista vem, nesta ordem de prioridade:
 *   1. variavel de ambiente FRONTEND_ORIGIN (varias origens separadas por virgula)
 *   2. init-param "origens" declarado no web.xml
 *   3. o padrao do Vite e do CRA (5173 e 3000)
 */
public class CorsFilter implements Filter {

    private static final String PADRAO = "http://localhost:5173,http://localhost:3000";

    private Set<String> origensPermitidas = Set.of();

    @Override
    public void init(FilterConfig config) {
        String configurado = System.getenv("FRONTEND_ORIGIN");
        if (configurado == null || configurado.isBlank()) {
            configurado = config.getInitParameter("origens");
        }
        if (configurado == null || configurado.isBlank()) {
            configurado = PADRAO;
        }

        origensPermitidas = Arrays.stream(configurado.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String origem = req.getHeader("Origin");
        if (origem != null && origensPermitidas.contains(origem)) {
            resp.setHeader("Access-Control-Allow-Origin", origem);
            resp.setHeader("Access-Control-Allow-Credentials", "true");
            resp.setHeader("Vary", "Origin");
        }

        // Preflight: responde aqui e nao deixa seguir para o filtro de autenticacao.
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            String cabecalhosPedidos = req.getHeader("Access-Control-Request-Headers");
            resp.setHeader("Access-Control-Allow-Headers",
                    cabecalhosPedidos != null ? cabecalhosPedidos : "Content-Type");
            resp.setHeader("Access-Control-Max-Age", "3600");
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }
}
