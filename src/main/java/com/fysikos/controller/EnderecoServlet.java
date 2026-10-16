package com.fysikos.controller;

import com.fysikos.dao.EnderecoDAO;
import com.fysikos.model.Endereco;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Reader;

@WebServlet("/api/enderecos/*")
public class EnderecoServlet extends HttpServlet {

    private final EnderecoDAO enderecoDAO = new EnderecoDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        String path = req.getPathInfo();

        if (path == null || path.equals("/")) {
            String usuarioId = req.getParameter("usuarioId");
            if (usuarioId == null) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"erro\":\"Parametro usuarioId e obrigatorio\"}");
                return;
            }
            resp.getWriter().write(gson.toJson(enderecoDAO.obterPorUsuario(Integer.parseInt(usuarioId))));
            return;
        }

        int id = Integer.parseInt(path.substring(1));
        Endereco endereco = enderecoDAO.obterPorId(id);

        if (endereco == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.getWriter().write(gson.toJson(endereco));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body;
        try (Reader reader = req.getReader()) {
            body = gson.fromJson(reader, JsonObject.class);
        }

        Endereco endereco = new Endereco();
        endereco.setUsuarioId(body.get("usuarioId").getAsInt());
        endereco.setCep(body.get("cep").getAsString());
        endereco.setLogradouro(body.get("logradouro").getAsString());
        endereco.setNumero(body.get("numero").getAsString());
        endereco.setComplemento(body.has("complemento") ? body.get("complemento").getAsString() : null);
        endereco.setBairro(body.get("bairro").getAsString());
        endereco.setCidade(body.get("cidade").getAsString());
        endereco.setEstado(body.get("estado").getAsString());
        endereco.setPrincipal(body.has("principal") && body.get("principal").getAsBoolean());

        enderecoDAO.inserir(endereco);

        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write(gson.toJson(endereco));
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        int id = Integer.parseInt(path.substring(1));
        Endereco endereco = enderecoDAO.obterPorId(id);
        if (endereco == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try (Reader reader = req.getReader()) {
            JsonObject body = gson.fromJson(reader, JsonObject.class);
            if (body.has("cep")) endereco.setCep(body.get("cep").getAsString());
            if (body.has("logradouro")) endereco.setLogradouro(body.get("logradouro").getAsString());
            if (body.has("numero")) endereco.setNumero(body.get("numero").getAsString());
            if (body.has("complemento")) endereco.setComplemento(body.get("complemento").getAsString());
            if (body.has("bairro")) endereco.setBairro(body.get("bairro").getAsString());
            if (body.has("cidade")) endereco.setCidade(body.get("cidade").getAsString());
            if (body.has("estado")) endereco.setEstado(body.get("estado").getAsString());
            if (body.has("principal")) endereco.setPrincipal(body.get("principal").getAsBoolean());
        }

        enderecoDAO.atualizar(endereco);
        resp.setContentType("application/json");
        resp.getWriter().write(gson.toJson(endereco));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null || path.equals("/")) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        int id = Integer.parseInt(path.substring(1));
        boolean removido = enderecoDAO.remover(id);
        resp.setStatus(removido ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_NOT_FOUND);
    }
}
