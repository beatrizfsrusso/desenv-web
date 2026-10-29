package com.fysikos.controller;

import com.fysikos.dao.ImagemProdutoDAO;
import com.fysikos.dao.ProdutoDAO;
import com.fysikos.model.ImagemProduto;
import com.fysikos.util.CorpoInvalidoException;
import com.fysikos.util.CorpoJson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Imagens de produto. O campo caminho e apenas uma string (URL ou caminho
 * relativo): nao ha upload de arquivo nesta entrega.
 *
 * A rota e /api/imagens em vez de /api/produtos/{id}/imagens para nao colidir
 * com o mapeamento /api/produtos/* do ProdutoServlet.
 *
 * Escrita restrita a administradores pelo AutenticacaoFilter.
 */
@WebServlet("/api/imagens/*")
public class ImagemProdutoServlet extends HttpServlet {

    private final ImagemProdutoDAO imagemDAO = new ImagemProdutoDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String produtoId = req.getParameter("produtoId");
        if (produtoId == null || produtoId.isBlank()) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Parametro produtoId e obrigatorio");
            return;
        }

        try {
            Resposta.ok(resp, imagemDAO.obterPorProduto(Integer.parseInt(produtoId)));
        } catch (NumberFormatException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "produtoId deve ser um numero inteiro");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            CorpoJson corpo = CorpoJson.ler(req);

            int produtoId = corpo.inteiroObrigatorio("produtoId");
            if (produtoDAO.obterPorId(produtoId) == null) {
                Resposta.erroNaoEncontrado(resp, "Produto nao encontrado");
                return;
            }

            ImagemProduto imagem = new ImagemProduto();
            imagem.setProdutoId(produtoId);
            imagem.setCaminho(corpo.textoObrigatorio("caminho"));
            imagem.setPrincipal(corpo.booleano("principal", false));

            imagemDAO.inserir(imagem);
            Resposta.criado(resp, imagem);

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.length() < 2) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Informe o id da imagem na URL");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(path.substring(1));
        } catch (NumberFormatException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Identificador invalido na URL");
            return;
        }

        if (imagemDAO.remover(id)) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            Resposta.erroNaoEncontrado(resp, "Imagem nao encontrada");
        }
    }
}
