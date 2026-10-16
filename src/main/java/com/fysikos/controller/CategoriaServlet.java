package com.fysikos.controller;

import com.fysikos.dao.CategoriaDAO;
import com.fysikos.model.Categoria;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Reader;

@WebServlet("/api/categorias/*")
public class CategoriaServlet extends HttpServlet {

    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            resp.getWriter().write(gson.toJson(categoriaDAO.obterTodas()));
            return;
        }

        int id = Integer.parseInt(path.substring(1));
        Categoria categoria = categoriaDAO.obterPorId(id);

        if (categoria == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.getWriter().write(gson.toJson(categoria));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body;
        try (Reader reader = req.getReader()) {
            body = gson.fromJson(reader, JsonObject.class);
        }

        Categoria categoria = new Categoria();
        categoria.setNome(body.get("nome").getAsString());
        categoriaDAO.inserir(categoria);

        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write(gson.toJson(categoria));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        int id = Integer.parseInt(path.substring(1));
        Categoria categoria = categoriaDAO.obterPorId(id);
        if (categoria == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try (Reader reader = req.getReader()) {
            JsonObject body = gson.fromJson(reader, JsonObject.class);
            categoria.setNome(body.get("nome").getAsString());
        }

        categoriaDAO.atualizar(categoria);
        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(categoria));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        int id = Integer.parseInt(path.substring(1));
        boolean removido = categoriaDAO.remover(id);
        resp.setStatus(removido ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_NOT_FOUND);
    }
}
