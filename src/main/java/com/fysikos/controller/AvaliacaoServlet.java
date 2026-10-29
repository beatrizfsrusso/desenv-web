package com.fysikos.controller;

import com.fysikos.dao.AvaliacaoDAO;
import com.fysikos.dao.ProdutoDAO;
import com.fysikos.model.Avaliacao;
import com.fysikos.util.CorpoInvalidoException;
import com.fysikos.util.CorpoJson;
import com.fysikos.util.Sessao;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Avaliacoes de produto. A leitura e publica; escrever exige login e apagar
 * exige ser o autor da avaliacao (ou administrador).
 *
 * A tabela nao tem id — a chave e (usuario_id, produto_id) — entao a avaliacao
 * nao e endereçada por id na URL:
 *
 *   GET    /api/avaliacoes?produtoId=N          lista as avaliacoes do produto
 *   POST   /api/avaliacoes                      {produtoId, nota, descricao, comentario?}
 *   DELETE /api/avaliacoes?produtoId=N          apaga a propria avaliacao
 *   DELETE /api/avaliacoes?produtoId=N&usuarioId=M   admin apaga a de outro
 *
 * Apagar e um soft delete: o DAO marca ativo = FALSE, preservando o historico.
 */
@WebServlet("/api/avaliacoes/*")
public class AvaliacaoServlet extends HttpServlet {

    private static final int NOTA_MINIMA = 1;
    private static final int NOTA_MAXIMA = 5;

    private final AvaliacaoDAO avaliacaoDAO = new AvaliacaoDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Integer produtoId = inteiroDaQuery(req, "produtoId");
            if (produtoId == null) {
                Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Parametro produtoId e obrigatorio");
                return;
            }

            Resposta.ok(resp, avaliacaoDAO.obterPorProduto(produtoId));

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer usuarioId = Sessao.usuarioId(req);

        try {
            CorpoJson corpo = CorpoJson.ler(req);

            int produtoId = corpo.inteiroObrigatorio("produtoId");
            if (produtoDAO.obterPorId(produtoId) == null) {
                Resposta.erroNaoEncontrado(resp, "Produto nao encontrado");
                return;
            }

            int nota = corpo.inteiroObrigatorio("nota");
            if (nota < NOTA_MINIMA || nota > NOTA_MAXIMA) {
                Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "A nota deve estar entre " + NOTA_MINIMA + " e " + NOTA_MAXIMA);
                return;
            }

            Avaliacao avaliacao = new Avaliacao();
            avaliacao.setUsuarioId(usuarioId);
            avaliacao.setProdutoId(produtoId);
            avaliacao.setNota(nota);
            avaliacao.setDescricao(corpo.textoObrigatorio("descricao"));
            avaliacao.setComentario(corpo.texto("comentario"));
            avaliacao.setData(LocalDate.now());
            avaliacao.setAtivo(true);

            avaliacaoDAO.inserir(avaliacao);
            Resposta.criado(resp, avaliacao);

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Integer produtoId = inteiroDaQuery(req, "produtoId");
            if (produtoId == null) {
                Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Parametro produtoId e obrigatorio");
                return;
            }

            // Sem usuarioId, apaga a propria. Com ele, so um administrador pode apontar
            // para outra pessoa.
            Integer usuarioId = inteiroDaQuery(req, "usuarioId");
            if (usuarioId == null) {
                usuarioId = Sessao.usuarioId(req);
            } else if (!Sessao.administrador(req)) {
                Resposta.erro(resp, HttpServletResponse.SC_FORBIDDEN,
                        "Voce so pode apagar suas proprias avaliacoes");
                return;
            }

            Avaliacao avaliacao = avaliacaoDAO.obterPorChave(usuarioId, produtoId);
            if (avaliacao == null || !avaliacao.isAtivo()) {
                Resposta.erroNaoEncontrado(resp, "Avaliacao nao encontrada");
                return;
            }

            avaliacaoDAO.remover(usuarioId, produtoId);
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    /** Le um parametro de query inteiro. Devolve null se ausente; 400 se nao for numero. */
    private Integer inteiroDaQuery(HttpServletRequest req, String nome) throws CorpoInvalidoException {
        String valor = req.getParameter(nome);
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new CorpoInvalidoException(nome + " deve ser um numero inteiro");
        }
    }
}
