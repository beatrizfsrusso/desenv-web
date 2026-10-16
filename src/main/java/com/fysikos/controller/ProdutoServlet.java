package com.fysikos.controller;

import com.fysikos.dao.ProdutoDAO;
import com.fysikos.model.Produto;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.List;

@WebServlet("/api/produtos/*")
public class ProdutoServlet extends HttpServlet {

    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            String busca = req.getParameter("busca");
            String categoriaId = req.getParameter("categoriaId");

            List<Produto> produtos;
            if (busca != null) {
                produtos = produtoDAO.buscarPorNome(busca);
            } else if (categoriaId != null) {
                produtos = produtoDAO.obterPorCategoria(Integer.parseInt(categoriaId));
            } else {
                produtos = produtoDAO.obterTodos();
            }

            resp.getWriter().write(gson.toJson(produtos));
            return;
        }

        int id = Integer.parseInt(path.substring(1));
        Produto produto = produtoDAO.obterPorId(id);

        if (produto == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.getWriter().write(gson.toJson(produto));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body;
        try (Reader reader = req.getReader()) {
            body = gson.fromJson(reader, JsonObject.class);
        }

        Produto produto = new Produto();
        produto.setCategoriaId(body.get("categoriaId").getAsInt());
        produto.setNome(body.get("nome").getAsString());
        produto.setDescricao(body.get("descricao").getAsString());
        produto.setPreco(new BigDecimal(body.get("preco").getAsString()));
        produto.setQuantidadeEstoque(body.get("quantidadeEstoque").getAsInt());
        produto.setAtivo(true);

        produtoDAO.inserir(produto);

        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write(gson.toJson(produto));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        int id = Integer.parseInt(path.substring(1));
        Produto produto = produtoDAO.obterPorId(id);
        if (produto == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try (Reader reader = req.getReader()) {
            JsonObject body = gson.fromJson(reader, JsonObject.class);
            if (body.has("categoriaId")) produto.setCategoriaId(body.get("categoriaId").getAsInt());
            if (body.has("nome")) produto.setNome(body.get("nome").getAsString());
            if (body.has("descricao")) produto.setDescricao(body.get("descricao").getAsString());
            if (body.has("preco")) produto.setPreco(new BigDecimal(body.get("preco").getAsString()));
            if (body.has("quantidadeEstoque")) produto.setQuantidadeEstoque(body.get("quantidadeEstoque").getAsInt());
            if (body.has("ativo")) produto.setAtivo(body.get("ativo").getAsBoolean());
        }

        produtoDAO.atualizar(produto);
        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(produto));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        int id = Integer.parseInt(path.substring(1));
        boolean removido = produtoDAO.remover(id);
        resp.setStatus(removido ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_NOT_FOUND);
    }
}
