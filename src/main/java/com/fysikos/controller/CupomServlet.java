package com.fysikos.controller;

import com.fysikos.dao.CupomDAO;
import com.fysikos.model.Cupom;
import com.fysikos.service.CheckoutService;
import com.fysikos.util.CorpoInvalidoException;
import com.fysikos.util.CorpoJson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Locale;

/**
 * CRUD de cupons. A escrita e restrita a administradores pelo AutenticacaoFilter.
 *
 * tipo_desconto aceita PERCENTUAL (valor = % sobre o subtotal) ou FIXO
 * (valor = desconto em reais).
 */
@WebServlet("/api/cupons/*")
public class CupomServlet extends HttpServlet {

    private final CupomDAO cupomDAO = new CupomDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            String codigo = req.getParameter("codigo");
            if (codigo != null && !codigo.isBlank()) {
                Cupom cupom = cupomDAO.obterPorCodigo(codigo.trim());
                if (cupom == null) {
                    Resposta.erroNaoEncontrado(resp, "Cupom nao encontrado");
                    return;
                }
                Resposta.ok(resp, cupom);
                return;
            }
            Resposta.ok(resp, cupomDAO.obterTodos());
            return;
        }

        Integer id = idDaRota(path);
        if (id == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Identificador invalido na URL");
            return;
        }

        Cupom cupom = cupomDAO.obterPorId(id);
        if (cupom == null) {
            Resposta.erroNaoEncontrado(resp, "Cupom nao encontrado");
            return;
        }
        Resposta.ok(resp, cupom);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            CorpoJson corpo = CorpoJson.ler(req);

            Cupom cupom = new Cupom();
            cupom.setCodigo(corpo.textoObrigatorio("codigo").toUpperCase(Locale.ROOT));
            cupom.setTipoDesconto(tipoValido(corpo.textoObrigatorio("tipoDesconto")));
            cupom.setValor(corpo.decimalObrigatorio("valor"));
            cupom.setValidade(corpo.dataObrigatoria("validade"));
            cupom.setAtivo(corpo.booleano("ativo", true));

            if (cupom.getValor().signum() <= 0) {
                Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "O valor do cupom deve ser maior que zero");
                return;
            }

            cupomDAO.inserir(cupom);
            Resposta.criado(resp, cupom);

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer id = idDaRota(req.getPathInfo());
        if (id == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Informe o id do cupom na URL");
            return;
        }

        Cupom cupom = cupomDAO.obterPorId(id);
        if (cupom == null) {
            Resposta.erroNaoEncontrado(resp, "Cupom nao encontrado");
            return;
        }

        try {
            CorpoJson corpo = CorpoJson.ler(req);
            if (corpo.tem("tipoDesconto")) {
                cupom.setTipoDesconto(tipoValido(corpo.textoObrigatorio("tipoDesconto")));
            }
            if (corpo.tem("valor")) {
                cupom.setValor(corpo.decimalObrigatorio("valor"));
            }
            if (corpo.tem("validade")) {
                cupom.setValidade(corpo.dataObrigatoria("validade"));
            }
            if (corpo.tem("ativo")) {
                cupom.setAtivo(corpo.booleano("ativo", cupom.isAtivo()));
            }

            cupomDAO.atualizar(cupom);
            Resposta.ok(resp, cupom);

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer id = idDaRota(req.getPathInfo());
        if (id == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Informe o id do cupom na URL");
            return;
        }

        if (cupomDAO.remover(id)) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            Resposta.erroNaoEncontrado(resp, "Cupom nao encontrado");
        }
    }

    private String tipoValido(String tipo) throws CorpoInvalidoException {
        String normalizado = tipo.trim().toUpperCase(Locale.ROOT);
        if (!CheckoutService.TIPO_DESCONTO_PERCENTUAL.equals(normalizado)
                && !CheckoutService.TIPO_DESCONTO_FIXO.equals(normalizado)) {
            throw new CorpoInvalidoException(
                    "tipoDesconto deve ser " + CheckoutService.TIPO_DESCONTO_PERCENTUAL
                            + " ou " + CheckoutService.TIPO_DESCONTO_FIXO);
        }
        return normalizado;
    }

    /** Extrai o id de uma rota tipo "/3". Devolve null se nao for um numero. */
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
