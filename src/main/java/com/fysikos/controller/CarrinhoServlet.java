package com.fysikos.controller;

import com.fysikos.dao.CarrinhoDAO;
import com.fysikos.dao.ProdutoDAO;
import com.fysikos.model.Carrinho;
import com.fysikos.model.ItemCarrinho;
import com.fysikos.model.Produto;
import com.fysikos.util.CorpoInvalidoException;
import com.fysikos.util.CorpoJson;
import com.fysikos.util.Sessao;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Carrinho do usuario logado. Nao ha id na rota: o carrinho e sempre o da
 * sessao, entao um usuario nao consegue mexer no carrinho de outro.
 *
 * Rotas:
 *   GET    /api/carrinho              carrinho com itens e total
 *   POST   /api/carrinho/itens        adiciona {produtoId, quantidade}
 *   PUT    /api/carrinho/itens/{id}   define a quantidade {quantidade}
 *   DELETE /api/carrinho/itens/{id}   remove o item
 *   DELETE /api/carrinho              esvazia o carrinho
 */
@WebServlet("/api/carrinho/*")
public class CarrinhoServlet extends HttpServlet {

    private static final String PREFIXO_ITENS = "/itens/";

    private final CarrinhoDAO carrinhoDAO = new CarrinhoDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Resposta.ok(resp, carrinhoDAO.obterOuCriar(Sessao.usuarioId(req)));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || !path.equals("/itens")) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Use POST /api/carrinho/itens para adicionar um produto");
            return;
        }

        try {
            CorpoJson corpo = CorpoJson.ler(req);
            int produtoId = corpo.inteiroObrigatorio("produtoId");
            int quantidade = corpo.inteiroObrigatorio("quantidade");

            if (quantidade <= 0) {
                Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "A quantidade deve ser maior que zero");
                return;
            }

            Produto produto = produtoDAO.obterPorId(produtoId);
            if (produto == null || !produto.isAtivo()) {
                Resposta.erroNaoEncontrado(resp, "Produto nao encontrado");
                return;
            }

            int usuarioId = Sessao.usuarioId(req);
            Carrinho carrinho = carrinhoDAO.obterOuCriar(usuarioId);

            // O estoque e checado sobre o total que ficaria no carrinho, nao
            // apenas sobre a quantidade recebida agora.
            int jaNoCarrinho = carrinho.getItens().stream()
                    .filter(item -> item.getProdutoId() == produtoId)
                    .mapToInt(ItemCarrinho::getQuantidade)
                    .findFirst()
                    .orElse(0);

            if (jaNoCarrinho + quantidade > produto.getQuantidadeEstoque()) {
                Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Estoque insuficiente: disponivel " + produto.getQuantidadeEstoque()
                                + ", solicitado " + (jaNoCarrinho + quantidade));
                return;
            }

            carrinhoDAO.adicionarItem(carrinho.getId(), produtoId, quantidade);
            Resposta.ok(resp, carrinhoDAO.obterOuCriar(usuarioId));

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer produtoId = produtoIdDaRota(req.getPathInfo());
        if (produtoId == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Use PUT /api/carrinho/itens/{produtoId}");
            return;
        }

        try {
            CorpoJson corpo = CorpoJson.ler(req);
            int quantidade = corpo.inteiroObrigatorio("quantidade");
            int usuarioId = Sessao.usuarioId(req);
            Carrinho carrinho = carrinhoDAO.obterOuCriar(usuarioId);

            // Quantidade zero ou negativa e o jeito natural de "tirar do carrinho".
            if (quantidade <= 0) {
                carrinhoDAO.removerItem(carrinho.getId(), produtoId);
                Resposta.ok(resp, carrinhoDAO.obterOuCriar(usuarioId));
                return;
            }

            Produto produto = produtoDAO.obterPorId(produtoId);
            if (produto == null || !produto.isAtivo()) {
                Resposta.erroNaoEncontrado(resp, "Produto nao encontrado");
                return;
            }
            if (quantidade > produto.getQuantidadeEstoque()) {
                Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "Estoque insuficiente: disponivel " + produto.getQuantidadeEstoque());
                return;
            }

            if (!carrinhoDAO.atualizarQuantidade(carrinho.getId(), produtoId, quantidade)) {
                Resposta.erroNaoEncontrado(resp, "Este produto nao esta no carrinho");
                return;
            }

            Resposta.ok(resp, carrinhoDAO.obterOuCriar(usuarioId));

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int usuarioId = Sessao.usuarioId(req);
        Carrinho carrinho = carrinhoDAO.obterOuCriar(usuarioId);
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            carrinhoDAO.limpar(carrinho.getId());
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        Integer produtoId = produtoIdDaRota(path);
        if (produtoId == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Identificador invalido na URL");
            return;
        }

        if (carrinhoDAO.removerItem(carrinho.getId(), produtoId)) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            Resposta.erroNaoEncontrado(resp, "Este produto nao esta no carrinho");
        }
    }

    /** Extrai o produtoId de uma rota tipo "/itens/7". */
    private Integer produtoIdDaRota(String path) {
        if (path == null || !path.startsWith(PREFIXO_ITENS)) {
            return null;
        }
        try {
            return Integer.parseInt(path.substring(PREFIXO_ITENS.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
