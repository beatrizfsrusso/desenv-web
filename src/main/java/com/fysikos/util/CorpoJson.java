package com.fysikos.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Leitura do corpo JSON com validacao. Cada campo obrigatorio ausente ou
 * invalido vira uma {@link CorpoInvalidoException} com mensagem em portugues,
 * que o servlet converte em 400.
 */
public class CorpoJson {

    private final JsonObject json;

    private CorpoJson(JsonObject json) {
        this.json = json;
    }

    public static CorpoJson ler(HttpServletRequest req) throws IOException, CorpoInvalidoException {
        try (Reader reader = req.getReader()) {
            JsonElement raiz = JsonParser.parseReader(reader);
            if (raiz == null || !raiz.isJsonObject()) {
                throw new CorpoInvalidoException("O corpo da requisicao deve ser um objeto JSON");
            }
            return new CorpoJson(raiz.getAsJsonObject());
        } catch (JsonSyntaxException e) {
            throw new CorpoInvalidoException("O corpo da requisicao nao e um JSON valido");
        }
    }

    public boolean tem(String campo) {
        JsonElement el = json.get(campo);
        return el != null && !el.isJsonNull();
    }

    public String textoObrigatorio(String campo) throws CorpoInvalidoException {
        String valor = texto(campo);
        if (valor == null || valor.isBlank()) {
            throw new CorpoInvalidoException("Campo obrigatorio ausente: " + campo);
        }
        return valor.trim();
    }

    /** Devolve null quando o campo nao veio. */
    public String texto(String campo) {
        JsonElement el = json.get(campo);
        if (el == null || el.isJsonNull()) {
            return null;
        }
        try {
            return el.getAsString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    public int inteiroObrigatorio(String campo) throws CorpoInvalidoException {
        JsonElement el = json.get(campo);
        if (el == null || el.isJsonNull()) {
            throw new CorpoInvalidoException("Campo obrigatorio ausente: " + campo);
        }
        try {
            return el.getAsInt();
        } catch (NumberFormatException | UnsupportedOperationException e) {
            throw new CorpoInvalidoException("O campo " + campo + " deve ser um numero inteiro");
        }
    }

    public Integer inteiro(String campo) throws CorpoInvalidoException {
        if (!tem(campo)) {
            return null;
        }
        return inteiroObrigatorio(campo);
    }

    public BigDecimal decimalObrigatorio(String campo) throws CorpoInvalidoException {
        JsonElement el = json.get(campo);
        if (el == null || el.isJsonNull()) {
            throw new CorpoInvalidoException("Campo obrigatorio ausente: " + campo);
        }
        try {
            return el.getAsBigDecimal();
        } catch (NumberFormatException | UnsupportedOperationException e) {
            throw new CorpoInvalidoException("O campo " + campo + " deve ser um numero decimal");
        }
    }

    public LocalDate dataObrigatoria(String campo) throws CorpoInvalidoException {
        String valor = textoObrigatorio(campo);
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw new CorpoInvalidoException("O campo " + campo + " deve estar no formato AAAA-MM-DD");
        }
    }

    public boolean booleano(String campo, boolean padrao) {
        JsonElement el = json.get(campo);
        if (el == null || el.isJsonNull()) {
            return padrao;
        }
        try {
            return el.getAsBoolean();
        } catch (RuntimeException e) {
            return padrao;
        }
    }
}
