package com.fysikos.util;

import com.fysikos.model.Carrinho;
import com.fysikos.model.ItemCarrinho;
import com.fysikos.model.ItemPedido;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gson configurado para a API.
 *
 * Duas razoes para nao usar o Gson puro:
 *
 *  1. java.time — sem adaptador, LocalDate e LocalDateTime saem como objetos
 *     internos ({"year":2026,"month":10,...}) em vez de data legivel.
 *
 *  2. Valores calculados — o Gson serializa campos, nao getters. Carrinho.total,
 *     ItemCarrinho.subtotal e ItemPedido.subtotal sao derivados (nao existem como
 *     coluna no banco), entao precisam de serializador proprio ou nao apareceriam
 *     na resposta.
 */
public class GsonFactory {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final Gson INSTANCIA = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new AdaptadorLocalDate())
            .registerTypeAdapter(LocalDateTime.class, new AdaptadorLocalDateTime())
            .registerTypeAdapter(Carrinho.class, (JsonSerializer<Carrinho>) (carrinho, tipo, ctx) -> {
                JsonObject json = new JsonObject();
                json.addProperty("id", carrinho.getId());
                json.addProperty("usuarioId", carrinho.getUsuarioId());
                JsonArray itens = new JsonArray();
                for (ItemCarrinho item : carrinho.getItens()) {
                    itens.add(ctx.serialize(item));
                }
                json.add("itens", itens);
                json.addProperty("total", carrinho.getTotal());
                return json;
            })
            .registerTypeAdapter(ItemCarrinho.class, (JsonSerializer<ItemCarrinho>) (item, tipo, ctx) -> {
                JsonObject json = new JsonObject();
                json.addProperty("produtoId", item.getProdutoId());
                json.addProperty("nome", item.getNome());
                json.addProperty("quantidade", item.getQuantidade());
                json.addProperty("precoUnitario", item.getPrecoUnitario());
                json.addProperty("subtotal", item.getSubtotal());
                return json;
            })
            .registerTypeAdapter(ItemPedido.class, (JsonSerializer<ItemPedido>) (item, tipo, ctx) -> {
                JsonObject json = new JsonObject();
                json.addProperty("produtoId", item.getProdutoId());
                json.addProperty("nome", item.getNome());
                json.addProperty("quantidade", item.getQuantidade());
                json.addProperty("precoUnitario", item.getPrecoUnitario());
                json.addProperty("subtotal", item.getSubtotal());
                return json;
            })
            .serializeNulls()
            .create();

    private GsonFactory() {
    }

    public static Gson get() {
        return INSTANCIA;
    }

    private static class AdaptadorLocalDate extends TypeAdapter<LocalDate> {

        @Override
        public void write(JsonWriter out, LocalDate valor) throws IOException {
            if (valor == null) {
                out.nullValue();
            } else {
                out.value(DATA.format(valor));
            }
        }

        @Override
        public LocalDate read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return LocalDate.parse(in.nextString(), DATA);
        }
    }

    private static class AdaptadorLocalDateTime extends TypeAdapter<LocalDateTime> {

        @Override
        public void write(JsonWriter out, LocalDateTime valor) throws IOException {
            if (valor == null) {
                out.nullValue();
            } else {
                out.value(DATA_HORA.format(valor));
            }
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return LocalDateTime.parse(in.nextString(), DATA_HORA);
        }
    }
}
