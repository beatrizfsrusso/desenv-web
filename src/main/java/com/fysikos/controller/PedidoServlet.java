package com.fysikos.controller;

import com.fysikos.dao.PedidoDAO;
import com.fysikos.model.Pedido;
import com.fysikos.service.CheckoutService;
import com.fysikos.util.CorpoInvalidoException;
import com.fysikos.util.CorpoJson;
import com.fysikos.util.Sessao;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Pedidos e checkout.
 *
 *   GET  /api/pedidos               pedidos do usuario logado
 *   GET  /api/pedidos?usuarioId=N   so administrador (ou o proprio usuario)
 *   GET  /api/pedidos/{id}          detalhe com itens e pagamento
 *   POST /api/pedidos/checkout      fecha o carrinho
 */
@WebServlet("/api/pedidos/*")
public class PedidoServlet extends HttpServlet {

    private final PedidoDAO pedidoDAO = new PedidoDAO();
    private final CheckoutService checkoutService = new CheckoutService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            listar(req, resp);
            return;
        }

        Integer id = idDaRota(path);
        if (id == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Identificador invalido na URL");
            return;
        }

        Pedido pedido = pedidoDAO.obterPorId(id);
        if (pedido == null) {
            Resposta.erroNaoEncontrado(resp, "Pedido nao encontrado");
            return;
        }

        boolean dono = pedido.getUsuarioId() == Sessao.usuarioId(req);
        if (!dono && !Sessao.administrador(req)) {
            // 404 em vez de 403 para nao revelar que o pedido existe.
            Resposta.erroNaoEncontrado(resp, "Pedido nao encontrado");
            return;
        }

        Resposta.ok(resp, pedido);
    }

    private void listar(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String usuarioIdParam = req.getParameter("usuarioId");
        boolean administrador = Sessao.administrador(req);

        if (usuarioIdParam != null && !usuarioIdParam.isBlank()) {
            int alvo;
            try {
                alvo = Integer.parseInt(usuarioIdParam);
            } catch (NumberFormatException e) {
                Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "usuarioId deve ser um numero inteiro");
                return;
            }
            if (!administrador && alvo != Sessao.usuarioId(req)) {
                Resposta.erro(resp, HttpServletResponse.SC_FORBIDDEN, "Voce so pode ver seus proprios pedidos");
                return;
            }
            Resposta.ok(resp, pedidoDAO.obterPorUsuario(alvo));
            return;
        }

        if (administrador && "true".equalsIgnoreCase(req.getParameter("todos"))) {
            Resposta.ok(resp, pedidoDAO.obterTodos());
            return;
        }

        List<Pedido> pedidos = pedidoDAO.obterPorUsuario(Sessao.usuarioId(req));
        Resposta.ok(resp, pedidos);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || !path.equals("/checkout")) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Use POST /api/pedidos/checkout para fechar o pedido");
            return;
        }

        try {
            CorpoJson corpo = CorpoJson.ler(req);

            int enderecoId = corpo.inteiroObrigatorio("enderecoId");
            String cupomCodigo = corpo.texto("cupomCodigo");
            String metodoPagamento = corpo.texto("metodoPagamento");

            Pedido pedido = checkoutService.finalizar(
                    Sessao.usuarioId(req), enderecoId, cupomCodigo, metodoPagamento);

            Resposta.criado(resp, pedido);

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (CheckoutService.CheckoutException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
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
