-- Schema do Fysikos (E-Commerce) baseado no MER do documento de entrega.
-- Observacao: Uso_Cupom.venda_id foi corrigido para pedido_id (FK -> pedido).
-- usuario_id foi mantido: garante via PK que o mesmo usuario nao reuse o
-- mesmo cupom em pedidos diferentes (uma constraint que so olha pedido_id
-- nao impediria isso, pois cada pedido tem um id diferente).

CREATE TABLE usuario (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    cpf VARCHAR(14) UNIQUE,
    administrador BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE categoria (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL
);

CREATE TABLE produto (
    id SERIAL PRIMARY KEY,
    categoria_id INTEGER NOT NULL REFERENCES categoria(id),
    nome VARCHAR(150) NOT NULL,
    descricao TEXT NOT NULL,
    preco NUMERIC(10,2) NOT NULL,
    quantidade_estoque INTEGER NOT NULL DEFAULT 0,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE imagem_produto (
    id SERIAL PRIMARY KEY,
    produto_id INTEGER NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
    caminho VARCHAR(255) NOT NULL,
    principal BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE endereco (
    id SERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    cep VARCHAR(9) NOT NULL,
    logradouro VARCHAR(150) NOT NULL,
    numero VARCHAR(20) NOT NULL,
    complemento VARCHAR(100),
    bairro VARCHAR(100) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    estado VARCHAR(2) NOT NULL,
    principal BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE avaliacao (
    id SERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    produto_id INTEGER NOT NULL REFERENCES produto(id),
    descricao VARCHAR(255) NOT NULL,
    comentario TEXT,
    data DATE NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE cupom (
    id SERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    tipo_desconto VARCHAR(20) NOT NULL,
    valor NUMERIC(10,2) NOT NULL,
    validade DATE NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Tabelas de carrinho/pedido/pagamento: dependem do fluxo de checkout,
-- que ainda esta em definicao. Mantidas aqui para fechar o MER completo.

CREATE TABLE carrinho (
    id SERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL UNIQUE REFERENCES usuario(id)
);

CREATE TABLE produto_carrinho (
    carrinho_id INTEGER NOT NULL REFERENCES carrinho(id) ON DELETE CASCADE,
    produto_id INTEGER NOT NULL REFERENCES produto(id),
    quantidade INTEGER NOT NULL,
    PRIMARY KEY (carrinho_id, produto_id)
);

CREATE TABLE pedido (
    id SERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    endereco_id INTEGER NOT NULL REFERENCES endereco(id),
    data_hora TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL,
    valor_frete NUMERIC(10,2) NOT NULL,
    desconto NUMERIC(10,2) NOT NULL DEFAULT 0,
    valor_total NUMERIC(10,2) NOT NULL
);

CREATE TABLE produto_pedido (
    pedido_id INTEGER NOT NULL REFERENCES pedido(id) ON DELETE CASCADE,
    produto_id INTEGER NOT NULL REFERENCES produto(id),
    quantidade INTEGER NOT NULL,
    preco_unitario NUMERIC(10,2) NOT NULL,
    PRIMARY KEY (pedido_id, produto_id)
);

CREATE TABLE pagamento (
    id SERIAL PRIMARY KEY,
    pedido_id INTEGER NOT NULL REFERENCES pedido(id),
    metodo VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    valor NUMERIC(10,2) NOT NULL,
    data_pagamento TIMESTAMP NOT NULL
);

CREATE TABLE uso_cupom (
    id_cupom INTEGER NOT NULL REFERENCES cupom(id),
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    pedido_id INTEGER NOT NULL UNIQUE REFERENCES pedido(id),
    data_uso TIMESTAMP NOT NULL,
    PRIMARY KEY (id_cupom, usuario_id)
);
