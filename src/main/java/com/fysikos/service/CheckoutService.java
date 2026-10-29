package com.fysikos.service;

import com.fysikos.dao.CarrinhoDAO;
import com.fysikos.dao.ConnectionFactory;
import com.fysikos.dao.CupomDAO;
import com.fysikos.dao.EnderecoDAO;
import com.fysikos.dao.PagamentoDAO;
import com.fysikos.dao.PedidoDAO;
import com.fysikos.dao.ProdutoDAO;
import com.fysikos.model.Carrinho;
import com.fysikos.model.Cupom;
import com.fysikos.model.Endereco;
import com.fysikos.model.ItemCarrinho;
import com.fysikos.model.ItemPedido;
import com.fysikos.model.Pagamento;
import com.fysikos.model.Pedido;
import com.fysikos.model.Produto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fecha o carrinho: cria o pedido, baixa o estoque, registra o pagamento e
 * consome o cupom, tudo na mesma transacao.
 *
 * O pagamento e SIMULADO. Nao existe gateway, adquirente nem dado de cartao:
 * o registro nasce com status APROVADO so para o fluxo do trabalho ter um fim.
 * Trocar isso por uma integracao real significaria substituir apenas o trecho
 * marcado em {@link #finalizar}.
 */
public class CheckoutService {

    /**
     * Status do pedido ao sair do checkout. O schema do banco usa PAGO, ENVIADO,
     * ENTREGUE, AGUARDANDO_PAGAMENTO e CANCELADO; como o pagamento simulado ja
     * nasce aprovado, o pedido nasce PAGO.
     */
    public static final String STATUS_PEDIDO_PAGO = "PAGO";
    public static final String STATUS_PAGAMENTO_APROVADO = "APROVADO";

    public static final String TIPO_DESCONTO_PERCENTUAL = "PERCENTUAL";
    /** Valor em reais. O nome segue o que os dados do banco usam. */
    public static final String TIPO_DESCONTO_FIXO = "VALOR_FIXO";

    /**
     * Frete fixo, gratis acima do limite. O limite e R$ 80, nao R$ 200: e a
     * regra que os pedidos de exemplo do banco seguem (o pedido de R$ 79,90
     * paga os R$ 15,00 de frete, os de valor maior nao).
     */
    private static final BigDecimal FRETE_PADRAO = new BigDecimal("15.00");
    private static final BigDecimal FRETE_GRATIS_ACIMA_DE = new BigDecimal("80.00");

    private static final String METODO_PAGAMENTO_PADRAO = "PIX";

    private final CarrinhoDAO carrinhoDAO = new CarrinhoDAO();
    private final PedidoDAO pedidoDAO = new PedidoDAO();
    private final PagamentoDAO pagamentoDAO = new PagamentoDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final CupomDAO cupomDAO = new CupomDAO();
    private final EnderecoDAO enderecoDAO = new EnderecoDAO();

    /** Erro de regra de negocio: vira 400 na resposta, nao 500. */
    public static class CheckoutException extends Exception {
        public CheckoutException(String mensagem) {
            super(mensagem);
        }
    }

    public Pedido finalizar(int usuarioId, int enderecoId, String cupomCodigo, String metodoPagamento)
            throws CheckoutException {

        Connection conn = null;
        try {
            conn = ConnectionFactory.getConnection();
            conn.setAutoCommit(false);

            Carrinho carrinho = carrinhoDAO.obterOuCriar(conn, usuarioId);
            if (carrinho.getItens().isEmpty()) {
                throw new CheckoutException("O carrinho esta vazio");
            }

            Endereco endereco = enderecoDAO.obterPorId(conn, enderecoId);
            if (endereco == null || endereco.getUsuarioId() != usuarioId) {
                throw new CheckoutException("Endereco de entrega invalido");
            }

            BigDecimal subtotal = carrinho.getTotal();
            BigDecimal frete = subtotal.compareTo(FRETE_GRATIS_ACIMA_DE) >= 0
                    ? BigDecimal.ZERO
                    : FRETE_PADRAO;

            Cupom cupom = validarCupom(conn, cupomCodigo, usuarioId);
            BigDecimal desconto = cupom == null ? BigDecimal.ZERO : calcularDesconto(cupom, subtotal);

            BigDecimal total = subtotal.add(frete).subtract(desconto);
            if (total.signum() < 0) {
                total = BigDecimal.ZERO;
            }

            // Reserva o estoque antes de criar o pedido: se algum item faltar,
            // a excecao derruba a transacao inteira e nada fica pela metade.
            for (ItemCarrinho item : carrinho.getItens()) {
                Produto produto = produtoDAO.obterPorId(conn, item.getProdutoId());
                if (produto == null || !produto.isAtivo()) {
                    throw new CheckoutException("Produto indisponivel: " + item.getNome());
                }
                if (!produtoDAO.baixarEstoque(conn, item.getProdutoId(), item.getQuantidade())) {
                    throw new CheckoutException(
                            "Estoque insuficiente para: " + item.getNome() + " (disponivel: "
                                    + produto.getQuantidadeEstoque() + ")");
                }
            }

            Pedido pedido = new Pedido();
            pedido.setUsuarioId(usuarioId);
            pedido.setEnderecoId(enderecoId);
            pedido.setDataHora(LocalDateTime.now());
            pedido.setStatus(STATUS_PEDIDO_PAGO);
            pedido.setValorFrete(frete);
            pedido.setDesconto(desconto);
            pedido.setValorTotal(total);
            pedidoDAO.inserir(conn, pedido);

            List<ItemPedido> itens = new ArrayList<>();
            for (ItemCarrinho item : carrinho.getItens()) {
                itens.add(new ItemPedido(
                        item.getProdutoId(),
                        item.getNome(),
                        item.getQuantidade(),
                        item.getPrecoUnitario()));
            }
            pedidoDAO.inserirItens(conn, pedido.getId(), itens);

            // ---- Pagamento simulado: nenhuma cobranca acontece de verdade ----
            Pagamento pagamento = new Pagamento();
            pagamento.setPedidoId(pedido.getId());
            pagamento.setMetodo(normalizarMetodo(metodoPagamento));
            pagamento.setStatus(STATUS_PAGAMENTO_APROVADO);
            pagamento.setValor(total);
            pagamento.setDataPagamento(LocalDateTime.now());
            pagamentoDAO.inserir(conn, pagamento);

            if (cupom != null) {
                cupomDAO.registrarUso(conn, cupom.getId(), usuarioId, pedido.getId());
            }

            carrinhoDAO.limpar(conn, carrinho.getId());

            conn.commit();

            pedido.setItens(itens);
            pedido.setPagamento(pagamento);
            return pedido;

        } catch (CheckoutException e) {
            desfazer(conn);
            throw e;
        } catch (SQLException e) {
            desfazer(conn);
            throw new RuntimeException("Erro ao finalizar o pedido", e);
        } finally {
            fechar(conn);
        }
    }

    private Cupom validarCupom(Connection conn, String codigo, int usuarioId)
            throws SQLException, CheckoutException {

        if (codigo == null || codigo.isBlank()) {
            return null;
        }

        Cupom cupom = cupomDAO.obterPorCodigo(conn, codigo.trim());
        if (cupom == null || !cupom.isAtivo()) {
            throw new CheckoutException("Cupom inexistente ou inativo");
        }
        if (cupom.getValidade().isBefore(LocalDate.now())) {
            throw new CheckoutException("Cupom vencido em " + cupom.getValidade());
        }
        if (cupomDAO.jaUsado(conn, cupom.getId(), usuarioId)) {
            throw new CheckoutException("Voce ja usou este cupom");
        }
        return cupom;
    }

    /** PERCENTUAL desconta uma fatia do subtotal; FIXO desconta um valor, sem passar do subtotal. */
    private BigDecimal calcularDesconto(Cupom cupom, BigDecimal subtotal) {
        if (TIPO_DESCONTO_PERCENTUAL.equalsIgnoreCase(cupom.getTipoDesconto())) {
            return subtotal.multiply(cupom.getValor())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return cupom.getValor().min(subtotal);
    }

    private String normalizarMetodo(String metodo) {
        if (metodo == null || metodo.isBlank()) {
            return METODO_PAGAMENTO_PADRAO;
        }
        String limpo = metodo.trim().toUpperCase(Locale.ROOT);
        return limpo.length() > 30 ? limpo.substring(0, 30) : limpo;
    }

    private void desfazer(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.rollback();
        } catch (SQLException ignored) {
            // Se o rollback falhou, a conexao sera descartada de qualquer forma.
        }
    }

    private void fechar(Connection conn) {
        if (conn == null) {
            return;
        }
        try {
            conn.close();
        } catch (SQLException ignored) {
            // Nada util a fazer aqui.
        }
    }
}
