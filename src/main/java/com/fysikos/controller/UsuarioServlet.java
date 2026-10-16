package com.fysikos.controller;

import com.fysikos.dao.UsuarioDAO;
import com.fysikos.model.Usuario;
import com.fysikos.util.PasswordUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.Reader;

@WebServlet("/api/usuarios/*")
public class UsuarioServlet extends HttpServlet {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            resp.getWriter().write(gson.toJson(usuarioDAO.obterTodos()));
            return;
        }

        int id = Integer.parseInt(path.substring(1));
        Usuario usuario = usuarioDAO.obterPorId(id);

        if (usuario == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.getWriter().write(gson.toJson(usuario));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String path = req.getPathInfo();

        if ("/login".equals(path)) {
            login(req, resp);
            return;
        }

        cadastrar(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        int id = Integer.parseInt(path.substring(1));
        Usuario usuario = usuarioDAO.obterPorId(id);
        if (usuario == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try (Reader reader = req.getReader()) {
            JsonObject body = gson.fromJson(reader, JsonObject.class);
            if (body.has("nome")) usuario.setNome(body.get("nome").getAsString());
            if (body.has("email")) usuario.setEmail(body.get("email").getAsString());
            if (body.has("cpf")) usuario.setCpf(body.get("cpf").getAsString());
        }

        usuarioDAO.atualizar(usuario);
        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(usuario));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        int id = Integer.parseInt(path.substring(1));
        boolean removido = usuarioDAO.remover(id);
        resp.setStatus(removido ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_NOT_FOUND);
    }

    private void cadastrar(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body;
        try (Reader reader = req.getReader()) {
            body = gson.fromJson(reader, JsonObject.class);
        }

        if (usuarioDAO.obterPorEmail(body.get("email").getAsString()) != null) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write("{\"erro\":\"Email ja cadastrado\"}");
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setNome(body.get("nome").getAsString());
        usuario.setEmail(body.get("email").getAsString());
        usuario.setSenhaHash(PasswordUtil.hash(body.get("senha").getAsString()));
        usuario.setCpf(body.has("cpf") ? body.get("cpf").getAsString() : null);
        usuario.setAdministrador(false);

        usuarioDAO.inserir(usuario);

        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write(gson.toJson(usuario));
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body;
        try (Reader reader = req.getReader()) {
            body = gson.fromJson(reader, JsonObject.class);
        }

        Usuario usuario = usuarioDAO.obterPorEmail(body.get("email").getAsString());
        resp.setContentType("application/json");

        if (usuario == null || !PasswordUtil.verifica(body.get("senha").getAsString(), usuario.getSenhaHash())) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"erro\":\"Email ou senha invalidos\"}");
            return;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("usuarioId", usuario.getId());

        resp.getWriter().write(gson.toJson(usuario));
    }
}
