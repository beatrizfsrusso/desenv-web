package com.fysikos.controller;

import com.fysikos.dao.UsuarioDAO;
import com.fysikos.model.Usuario;
import com.fysikos.util.CorpoInvalidoException;
import com.fysikos.util.CorpoJson;
import com.fysikos.util.PasswordUtil;
import com.fysikos.util.Sessao;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * Usuarios, cadastro e sessao.
 *
 *   POST /api/usuarios          cadastro (publico)
 *   POST /api/usuarios/login    abre a sessao (publico)
 *   POST /api/usuarios/logout   encerra a sessao (publico)
 *   GET  /api/usuarios          lista todos (somente administrador)
 *   GET  /api/usuarios/{id}     o proprio usuario ou administrador
 *   PUT  /api/usuarios/{id}     o proprio usuario ou administrador
 *   DELETE /api/usuarios/{id}   o proprio usuario ou administrador
 *
 * A senha nunca aparece na resposta: Usuario.senhaHash e transient, entao o
 * Gson o ignora na serializacao.
 */
@WebServlet("/api/usuarios/*")
public class UsuarioServlet extends HttpServlet {

    private static final int TAMANHO_MINIMO_SENHA = 6;

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            if (!Sessao.administrador(req)) {
                Resposta.erro(resp, HttpServletResponse.SC_FORBIDDEN,
                        "Apenas administradores podem listar os usuarios");
                return;
            }
            List<Usuario> usuarios = usuarioDAO.obterTodos();
            Resposta.ok(resp, usuarios);
            return;
        }

        Integer id = idDaRota(path);
        if (id == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Identificador invalido na URL");
            return;
        }

        if (!podeMexerNaConta(req, id)) {
            Resposta.erro(resp, HttpServletResponse.SC_FORBIDDEN, "Voce so pode ver sua propria conta");
            return;
        }

        Usuario usuario = usuarioDAO.obterPorId(id);
        if (usuario == null) {
            Resposta.erroNaoEncontrado(resp, "Usuario nao encontrado");
            return;
        }
        Resposta.ok(resp, usuario);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        if ("/login".equals(path)) {
            login(req, resp);
            return;
        }
        if ("/logout".equals(path)) {
            logout(req, resp);
            return;
        }

        cadastrar(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer id = idDaRota(req.getPathInfo());
        if (id == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Informe o id do usuario na URL");
            return;
        }
        if (!podeMexerNaConta(req, id)) {
            Resposta.erro(resp, HttpServletResponse.SC_FORBIDDEN, "Voce so pode alterar sua propria conta");
            return;
        }

        Usuario usuario = usuarioDAO.obterPorId(id);
        if (usuario == null) {
            Resposta.erroNaoEncontrado(resp, "Usuario nao encontrado");
            return;
        }

        try {
            CorpoJson corpo = CorpoJson.ler(req);

            // Trocar email ou senha e uma acao sensivel (quem tem a sessao aberta
            // poderia tomar a conta), entao exige a senha atual. A checagem vem
            // antes de qualquer escrita: o bloco da senha grava na hora, sem
            // esperar o atualizar() do final.
            boolean trocaEmail = corpo.tem("email");
            boolean trocaSenha = corpo.tem("senha");
            if (ehPropriaConta(req, id) && (trocaEmail || trocaSenha)) {
                String senhaAtual = corpo.textoObrigatorio("senhaAtual");
                if (!PasswordUtil.verifica(senhaAtual, usuario.getSenhaHash())) {
                    Resposta.erro(resp, HttpServletResponse.SC_UNAUTHORIZED, "Senha atual incorreta");
                    return;
                }
            }

            if (corpo.tem("nome")) {
                usuario.setNome(corpo.textoObrigatorio("nome"));
            }
            if (corpo.tem("email")) {
                String email = corpo.textoObrigatorio("email");
                validarEmail(email);
                Usuario donoDoEmail = usuarioDAO.obterPorEmail(email);
                if (donoDoEmail != null && donoDoEmail.getId() != id) {
                    Resposta.erro(resp, HttpServletResponse.SC_CONFLICT, "Este email ja esta em uso");
                    return;
                }
                usuario.setEmail(email);
            }
            if (corpo.tem("cpf")) {
                usuario.setCpf(corpo.texto("cpf"));
            }
            if (corpo.tem("senha")) {
                String senha = corpo.textoObrigatorio("senha");
                validarSenha(senha);
                usuario.setSenhaHash(PasswordUtil.hash(senha));
                usuarioDAO.atualizarSenha(usuario.getId(), usuario.getSenhaHash());
            }

            usuarioDAO.atualizar(usuario);
            Resposta.ok(resp, usuario);

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer id = idDaRota(req.getPathInfo());
        if (id == null) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, "Informe o id do usuario na URL");
            return;
        }
        if (!podeMexerNaConta(req, id)) {
            Resposta.erro(resp, HttpServletResponse.SC_FORBIDDEN, "Voce so pode excluir sua propria conta");
            return;
        }

        if (!usuarioDAO.remover(id)) {
            Resposta.erroNaoEncontrado(resp, "Usuario nao encontrado");
            return;
        }

        // Se o usuario apagou a propria conta, a sessao nao faz mais sentido.
        HttpSession sessao = req.getSession(false);
        if (sessao != null && Integer.valueOf(id).equals(sessao.getAttribute(Sessao.ATRIBUTO_USUARIO_ID))) {
            sessao.invalidate();
        }
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private void cadastrar(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            CorpoJson corpo = CorpoJson.ler(req);

            String nome = corpo.textoObrigatorio("nome");
            String email = corpo.textoObrigatorio("email");
            String senha = corpo.textoObrigatorio("senha");
            validarEmail(email);
            validarSenha(senha);

            if (usuarioDAO.obterPorEmail(email) != null) {
                Resposta.erro(resp, HttpServletResponse.SC_CONFLICT, "Email ja cadastrado");
                return;
            }

            Usuario usuario = new Usuario();
            usuario.setNome(nome);
            usuario.setEmail(email);
            usuario.setSenhaHash(PasswordUtil.hash(senha));
            usuario.setCpf(corpo.texto("cpf"));
            // Administrador nunca vem do corpo da requisicao: so o seed cria um.
            usuario.setAdministrador(false);

            usuarioDAO.inserir(usuario);
            Resposta.criado(resp, usuario);

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            CorpoJson corpo = CorpoJson.ler(req);
            String email = corpo.textoObrigatorio("email");
            String senha = corpo.textoObrigatorio("senha");

            Usuario usuario = usuarioDAO.obterPorEmail(email);
            // Conta desativada entra no mesmo erro de credencial invalida: nao vale
            // revelar para quem tentou logar que aquele email existe mas foi excluido.
            if (usuario == null || !usuario.isAtivo() || !PasswordUtil.verifica(senha, usuario.getSenhaHash())) {
                Resposta.erro(resp, HttpServletResponse.SC_UNAUTHORIZED, "Email ou senha invalidos");
                return;
            }

            HttpSession sessao = req.getSession(true);
            sessao.setAttribute(Sessao.ATRIBUTO_USUARIO_ID, usuario.getId());
            sessao.setAttribute(Sessao.ATRIBUTO_ADMINISTRADOR, usuario.isAdministrador());

            Resposta.ok(resp, usuario);

        } catch (CorpoInvalidoException e) {
            Resposta.erro(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession sessao = req.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private boolean podeMexerNaConta(HttpServletRequest req, int idAlvo) {
        Integer usuarioId = Sessao.usuarioId(req);
        return Sessao.administrador(req) || (usuarioId != null && usuarioId == idAlvo);
    }

    /**
     * Diferente de {@link #podeMexerNaConta}: aqui nao vale ser administrador.
     * Um admin editando a conta de outra pessoa nao teria como informar a senha
     * dela, entao a confirmacao so se aplica quando e o dono mexendo na propria.
     */
    private boolean ehPropriaConta(HttpServletRequest req, int idAlvo) {
        Integer usuarioId = Sessao.usuarioId(req);
        return usuarioId != null && usuarioId == idAlvo;
    }

    private void validarEmail(String email) throws CorpoInvalidoException {
        if (!email.contains("@") || email.startsWith("@") || email.endsWith("@")) {
            throw new CorpoInvalidoException("Email invalido");
        }
    }

    private void validarSenha(String senha) throws CorpoInvalidoException {
        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new CorpoInvalidoException("A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres");
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
