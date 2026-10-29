package com.fysikos.controller;

import com.fysikos.dao.CategoriaDAO;
import com.fysikos.model.Categoria;
import com.fysikos.util.CorpoInvalidoException;
import com.fysikos.util.CorpoJson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Categorias. Leitura publica; escrita restrita a administradores pelo
 * AutenticacaoFilter.
 */
@WebServlet("/api/categorias/*")
public class CategoriaServlet extends HttpServlet {

    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            Resposta.ok(resp, categoriaDAO.obterTodas());
            return;
        }

        Integer id = idDaRota(path);
        if (id == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Identificador invalido na URL");
            return;
        }

        Categoria categoria = categoriaDAO.obterPorId(id);
        if (categoria == null) {
            Resposta.erroNaoEncontrado(resp, "Categoria nao encontrada");
            return;
        }
        Resposta.ok(resp, categoria);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            CorpoJson corpo = CorpoJson.ler(req);

            Categoria categoria = new Categoria();
            categoria.setNome(corpo.textoObrigatorio("nome"));

            categoriaDAO.inserir(categoria);
            Resposta.criado(resp, categoria);

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer id = idDaRota(req.getPathInfo());
        if (id == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Informe o id da categoria na URL");
            return;
        }

        Categoria categoria = categoriaDAO.obterPorId(id);
        if (categoria == null) {
            Resposta.erroNaoEncontrado(resp, "Categoria nao encontrada");
            return;
        }

        try {
            CorpoJson corpo = CorpoJson.ler(req);
            categoria.setNome(corpo.textoObrigatorio("nome"));

            categoriaDAO.atualizar(categoria);
            Resposta.ok(resp, categoria);

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer id = idDaRota(req.getPathInfo());
        if (id == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Informe o id da categoria na URL");
            return;
        }

        if (categoriaDAO.remover(id)) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            Resposta.erroNaoEncontrado(resp, "Categoria nao encontrada");
        }
    }

    private Integer idDaRota(String path) {
        if (path == null || path.length() < 2) {
            return null;
        }
        try {
            return Integer.parseInt(path.substring(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
