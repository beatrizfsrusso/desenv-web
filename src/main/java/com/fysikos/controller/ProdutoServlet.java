package com.fysikos.controller;

import com.fysikos.dao.CategoriaDAO;
import com.fysikos.dao.ProdutoDAO;
import com.fysikos.model.Produto;
import com.fysikos.util.CorpoInvalidoException;
import com.fysikos.util.CorpoJson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Catalogo de produtos. A leitura e publica (o front lista sem exigir login);
 * criar, alterar e remover sao restritos a administradores pelo
 * AutenticacaoFilter.
 *
 *   GET /api/produtos?busca=...&categoriaId=...
 */
@WebServlet("/api/produtos/*")
public class ProdutoServlet extends HttpServlet {

    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            String busca = req.getParameter("busca");
            String categoriaId = req.getParameter("categoriaId");

            List<Produto> produtos;
            try {
                if (busca != null && !busca.isBlank()) {
                    produtos = produtoDAO.buscarPorNome(busca.trim());
                } else if (categoriaId != null && !categoriaId.isBlank()) {
                    produtos = produtoDAO.obterPorCategoria(Integer.parseInt(categoriaId));
                } else {
                    produtos = produtoDAO.obterTodos();
                }
            } catch (NumberFormatException e) {
                Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "categoriaId deve ser um numero inteiro");
                return;
            }

            Resposta.ok(resp, produtos);
            return;
        }

        Integer id = idDaRota(path);
        if (id == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Identificador invalido na URL");
            return;
        }

        Produto produto = produtoDAO.obterPorId(id);
        if (produto == null) {
            Resposta.erroNaoEncontrado(resp, "Produto nao encontrado");
            return;
        }
        Resposta.ok(resp, produto);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            CorpoJson corpo = CorpoJson.ler(req);

            Produto produto = new Produto();
            produto.setCategoriaId(corpo.inteiroObrigatorio("categoriaId"));
            produto.setNome(corpo.textoObrigatorio("nome"));
            produto.setDescricao(corpo.textoObrigatorio("descricao"));
            produto.setPreco(corpo.decimalObrigatorio("preco"));
            produto.setQuantidadeEstoque(corpo.inteiroObrigatorio("quantidadeEstoque"));
            produto.setAtivo(corpo.booleano("ativo", true));

            String erro = validar(produto);
            if (erro != null) {
                Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, erro);
                return;
            }

            produtoDAO.inserir(produto);
            Resposta.criado(resp, produto);

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer id = idDaRota(req.getPathInfo());
        if (id == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Informe o id do produto na URL");
            return;
        }

        Produto produto = produtoDAO.obterPorId(id);
        if (produto == null) {
            Resposta.erroNaoEncontrado(resp, "Produto nao encontrado");
            return;
        }

        try {
            CorpoJson corpo = CorpoJson.ler(req);
            if (corpo.tem("categoriaId")) {
                produto.setCategoriaId(corpo.inteiroObrigatorio("categoriaId"));
            }
            if (corpo.tem("nome")) {
                produto.setNome(corpo.textoObrigatorio("nome"));
            }
            if (corpo.tem("descricao")) {
                produto.setDescricao(corpo.textoObrigatorio("descricao"));
            }
            if (corpo.tem("preco")) {
                produto.setPreco(corpo.decimalObrigatorio("preco"));
            }
            if (corpo.tem("quantidadeEstoque")) {
                produto.setQuantidadeEstoque(corpo.inteiroObrigatorio("quantidadeEstoque"));
            }
            if (corpo.tem("ativo")) {
                produto.setAtivo(corpo.booleano("ativo", produto.isAtivo()));
            }

            String erro = validar(produto);
            if (erro != null) {
                Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, erro);
                return;
            }

            produtoDAO.atualizar(produto);
            Resposta.ok(resp, produto);

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer id = idDaRota(req.getPathInfo());
        if (id == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Informe o id do produto na URL");
            return;
        }

        if (produtoDAO.remover(id)) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            Resposta.erroNaoEncontrado(resp, "Produto nao encontrado");
        }
    }

    /** Devolve a mensagem de erro, ou null se o produto estiver valido. */
    private String validar(Produto produto) {
        if (produto.getPreco() == null || produto.getPreco().signum() <= 0) {
            return "O preco deve ser maior que zero";
        }
        if (produto.getQuantidadeEstoque() < 0) {
            return "A quantidade em estoque nao pode ser negativa";
        }
        if (categoriaDAO.obterPorId(produto.getCategoriaId()) == null) {
            return "Categoria inexistente: " + produto.getCategoriaId();
        }
        return null;
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
