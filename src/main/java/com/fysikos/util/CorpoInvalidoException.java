package com.fysikos.util;

/**
 * Lancada quando o corpo da requisicao nao tem os campos esperados. Vira uma
 * resposta 400 no servlet, no lugar do NullPointerException que acontecia
 * quando um campo obrigatorio vinha faltando.
 */
public class CorpoInvalidoException extends Exception {

    public CorpoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
