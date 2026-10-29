package com.fysikos.controller;

import com.fysikos.dao.EnderecoDAO;
import com.fysikos.model.Endereco;
import com.fysikos.util.CorpoInvalidoException;
import com.fysikos.util.CorpoJson;
import com.fysikos.util.Sessao;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Enderecos de entrega do usuario.
 *
 * O usuarioId nunca vem do corpo: e sempre o da sessao. Isso impede que um
 * usuario crie ou consulte enderecos em nome de outro.
 *
 *   GET    /api/enderecos              enderecos do usuario logado
 *   GET    /api/enderecos?usuarioId=N  so o proprio usuario ou administrador
 *   POST   /api/enderecos              cria um endereco para o usuario logado
 *   PUT    /api/enderecos/{id}         dono ou administrador
 *   DELETE /api/enderecos/{id}         dono ou administrador
 */
@WebServlet("/api/enderecos/*")
public class EnderecoServlet extends HttpServlet {

    private final EnderecoDAO enderecoDAO = new EnderecoDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            String usuarioIdParam = req.getParameter("usuarioId");
            int alvo;

            if (usuarioIdParam == null || usuarioIdParam.isBlank()) {
                alvo = Sessao.usuarioId(req);
            } else {
                try {
                    alvo = Integer.parseInt(usuarioIdParam);
                } catch (NumberFormatException e) {
                    Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST,
                            "usuarioId deve ser um numero inteiro");
                    return;
                }
                if (!podeAcessar(req, alvo)) {
                    Resposta.erro(resp, HttpServletResponse.SC_FORBIDDEN,
                            "Voce so pode ver seus proprios enderecos");
                    return;
                }
            }

            Resposta.ok(resp, enderecoDAO.obterPorUsuario(alvo));
            return;
        }

        Integer id = idDaRota(path);
        if (id == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Identificador invalido na URL");
            return;
        }

        Endereco endereco = enderecoDAO.obterPorId(id);
        if (endereco == null) {
            Resposta.erroNaoEncontrado(resp, "Endereco nao encontrado");
            return;
        }
        if (!podeAcessar(req, endereco.getUsuarioId())) {
            Resposta.erroNaoEncontrado(resp, "Endereco nao encontrado");
            return;
        }
        Resposta.ok(resp, endereco);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            CorpoJson corpo = CorpoJson.ler(req);

            Endereco endereco = new Endereco();
            endereco.setUsuarioId(Sessao.usuarioId(req));
            endereco.setCep(corpo.textoObrigatorio("cep"));
            endereco.setLogradouro(corpo.textoObrigatorio("logradouro"));
            endereco.setNumero(corpo.textoObrigatorio("numero"));
            endereco.setComplemento(corpo.texto("complemento"));
            endereco.setBairro(corpo.textoObrigatorio("bairro"));
            endereco.setCidade(corpo.textoObrigatorio("cidade"));
            endereco.setEstado(corpo.textoObrigatorio("estado"));
            endereco.setPrincipal(corpo.booleano("principal", false));

            enderecoDAO.inserir(endereco);
            Resposta.criado(resp, endereco);

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer id = idDaRota(req.getPathInfo());
        if (id == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Informe o id do endereco na URL");
            return;
        }

        Endereco endereco = enderecoDAO.obterPorId(id);
        if (endereco == null) {
            Resposta.erroNaoEncontrado(resp, "Endereco nao encontrado");
            return;
        }
        if (!podeAcessar(req, endereco.getUsuarioId())) {
            Resposta.erroNaoEncontrado(resp, "Endereco nao encontrado");
            return;
        }

        try {
            CorpoJson corpo = CorpoJson.ler(req);
            if (corpo.tem("cep")) endereco.setCep(corpo.textoObrigatorio("cep"));
            if (corpo.tem("logradouro")) endereco.setLogradouro(corpo.textoObrigatorio("logradouro"));
            if (corpo.tem("numero")) endereco.setNumero(corpo.textoObrigatorio("numero"));
            if (corpo.tem("complemento")) endereco.setComplemento(corpo.texto("complemento"));
            if (corpo.tem("bairro")) endereco.setBairro(corpo.textoObrigatorio("bairro"));
            if (corpo.tem("cidade")) endereco.setCidade(corpo.textoObrigatorio("cidade"));
            if (corpo.tem("estado")) endereco.setEstado(corpo.textoObrigatorio("estado"));
            if (corpo.tem("principal")) endereco.setPrincipal(corpo.booleano("principal", endereco.isPrincipal()));

            enderecoDAO.atualizar(endereco);
            Resposta.ok(resp, endereco);

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer id = idDaRota(req.getPathInfo());
        if (id == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Informe o id do endereco na URL");
            return;
        }

        Endereco endereco = enderecoDAO.obterPorId(id);
        if (endereco == null || !podeAcessar(req, endereco.getUsuarioId())) {
            Resposta.erroNaoEncontrado(resp, "Endereco nao encontrado");
            return;
        }

        enderecoDAO.remover(id);
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private boolean podeAcessar(HttpServletRequest req, int donoId) {
        Integer usuarioId = Sessao.usuarioId(req);
        return Sessao.administrador(req) || (usuarioId != null && usuarioId == donoId);
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
