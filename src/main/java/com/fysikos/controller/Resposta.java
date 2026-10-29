package com.fysikos.controller;

import com.fysikos.util.GsonFactory;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Centraliza a escrita das respostas JSON para que todos os servlets devolvam
 * o mesmo formato de erro: {"erro": "mensagem"}.
 */
public final class Resposta {

    private static final Gson GSON = GsonFactory.get();

    private Resposta() {
    }

    public static void json(HttpServletResponse resp, int status, Object corpo) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(status);
        resp.getWriter().write(GSON.toJson(corpo));
    }

    public static void ok(HttpServletResponse resp, Object corpo) throws IOException {
        json(resp, HttpServletResponse.SC_OK, corpo);
    }

    public static void criado(HttpServletResponse resp, Object corpo) throws IOException {
        json(resp, HttpServletResponse.SC_CREATED, corpo);
    }

    public static void erro(HttpServletResponse resp, int status, String mensagem) throws IOException {
        json(resp, status, Map.of("erro", mensagem));
    }

    public static void erroNaoEncontrado(HttpServletResponse resp, String mensagem) throws IOException {
        erro(resp, HttpServletResponse.SC_NOT_FOUND, mensagem);
    }
}
