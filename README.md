# Fysikos — API do e-commerce

Backend REST do e-commerce Fysikos. Java 21, Servlets (Jakarta EE 10), JDBC puro,
PostgreSQL, Gson. Roda em Jetty embutido via plugin do Maven, na porta **8081**.

O front (React, feito a partir do protótipo no Figma) é um projeto separado e
consome esta API por HTTP, com a sessão trafegando em cookie.

## Como rodar

O banco — schema **e** dados de teste — vive no repositório **`ecommerce-data`**,
que é a fonte canônica. Este repositório não mantém schema próprio; ele só aponta
para lá.

1. Suba o banco, a partir do `ecommerce-data`:
   ```bash
   cd ../ecommerce-data
   docker compose up -d
   ```
   O Postgres fica em `localhost:5433`, banco `ecommerce`, usuário `postgres`. A
   senha vem do `.env` daquele repositório (`POSTGRES_PASSWORD`) — crie um a
   partir do `.env.example` se ainda não existir.

   O `db/` do `ecommerce-data` é montado em `docker-entrypoint-initdb.d`, e o
   Postgres só executa esse diretório **quando o volume está vazio**. Se o
   container já existia, aplique na mão, nesta ordem:
   ```bash
   docker exec -i ecommerce-db psql -U postgres -d ecommerce < db/01-schema.sql
   docker exec -i ecommerce-db psql -U postgres -d ecommerce < db/02-dados-teste.sql
   ```

2. Suba a API:
   ```bash
   mvn jetty:run
   ```

3. A API responde em `http://localhost:8081/api/...`.

Usuários criados pelo seed (senhas em BCrypt):

| Email | Senha | Perfil |
|---|---|---|
| `admin@fusikos.com` | `admin123` | administrador |
| `leticia@email.com` | `senha123` | cliente |
| `rafael@email.com` | `senha123` | cliente |
| `camila@email.com` | `senha123` | cliente |

## Variáveis de ambiente

| Variável | Padrão | Para que serve |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5433/ecommerce` | conexão JDBC |
| `DB_USER` | `postgres` | usuário do banco |
| `DB_PASSWORD` | `postgres` | senha do banco (a mesma do `.env` do `ecommerce-data`) |
| `FRONTEND_ORIGIN` | `http://localhost:5173,http://localhost:3000` | origens liberadas no CORS, separadas por vírgula |

## Autenticação

Sessão com cookie `JSESSIONID` (não há token/JWT). No login a API guarda o id e
a flag de administrador na sessão; o cookie é `HttpOnly`.

Como a API usa cookie, o front precisa enviar as requisições com credenciais:

```js
fetch("http://localhost:8081/api/carrinho", { credentials: "include" })
```

E a origem do front precisa estar em `FRONTEND_ORIGIN` — o CORS nunca responde
`*`, porque o navegador recusa `*` junto com credenciais.

### Quem pode o quê

| Rota | Sem login | Logado | Admin |
|---|---|---|---|
| `GET /api/produtos`, `/api/categorias`, `/api/avaliacoes`, `/api/imagens` | sim | sim | sim |
| `POST /api/usuarios` (cadastro), `/api/usuarios/login`, `/api/usuarios/logout` | sim | sim | sim |
| `GET/PUT/DELETE /api/usuarios/{id}` | não | só a própria conta | qualquer |
| `GET /api/usuarios` (listar todos) | não | não | sim |
| `/api/carrinho`, `/api/pedidos`, `/api/enderecos` | não | só os próprios | vê os de todos |
| `POST/PUT/DELETE` em produtos, categorias, cupons, imagens | não | não | sim |

## Endpoints

### Produtos
```
GET    /api/produtos                      lista (filtros: ?busca=  ?categoriaId=)
GET    /api/produtos/{id}
POST   /api/produtos                      admin
PUT    /api/produtos/{id}                 admin
DELETE /api/produtos/{id}                 admin
```

### Categorias
```
GET    /api/categorias
GET    /api/categorias/{id}
POST   /api/categorias                    admin
PUT    /api/categorias/{id}               admin
DELETE /api/categorias/{id}               admin
```

### Imagens de produto
O campo `caminho` é só uma string (URL ou caminho relativo) — não há upload.
```
GET    /api/imagens?produtoId=N
POST   /api/imagens                       admin   {produtoId, caminho, principal}
DELETE /api/imagens/{id}                  admin
```

### Usuários e sessão
```
POST   /api/usuarios                      cadastro  {nome, email, senha, cpf?}
POST   /api/usuarios/login                {email, senha}
POST   /api/usuarios/logout
GET    /api/usuarios                      admin
GET    /api/usuarios/{id}
PUT    /api/usuarios/{id}                 {nome?, email?, cpf?, senha?, senhaAtual?}
DELETE /api/usuarios/{id}                 desativa a conta (exclusao logica)
```

Trocar **email** ou **senha** na própria conta exige `senhaAtual` junto no corpo
(401 se não bater). Alterar só `nome`/`cpf` não exige. Um administrador editando
a conta de outra pessoa não precisa informar a senha dela.

Contas desativadas não logam (401) e não aparecem na listagem, mas o histórico
de pedidos, avaliações e o endereço continuam no banco — por isso a exclusão é
lógica, e não um `DELETE`.

### Endereços
```
GET    /api/enderecos                     os do usuário logado
GET    /api/enderecos?usuarioId=N         o próprio ou admin
POST   /api/enderecos                     {cep, logradouro, numero, complemento?, bairro, cidade, estado, principal?}
PUT    /api/enderecos/{id}
DELETE /api/enderecos/{id}
```

### Carrinho
```
GET    /api/carrinho                      devolve itens, subtotais e total
POST   /api/carrinho/itens                {produtoId, quantidade}
PUT    /api/carrinho/itens/{produtoId}    {quantidade}  (0 ou menos remove)
DELETE /api/carrinho/itens/{produtoId}
DELETE /api/carrinho                      esvazia
```

### Pedidos
```
GET    /api/pedidos                       os do usuário logado
GET    /api/pedidos?usuarioId=N           o próprio ou admin
GET    /api/pedidos?todos=true            admin
GET    /api/pedidos/{id}                  com itens e pagamento
POST   /api/pedidos/checkout              {enderecoId, cupomCodigo?, metodoPagamento?}
```

### Avaliações
A tabela `avaliacao` não tem `id` — a chave é `(usuario_id, produto_id)`, ou seja,
uma avaliação por usuário por produto. Por isso o alvo vai na query string e não
no caminho.

```
GET    /api/avaliacoes?produtoId=N               lista as do produto
POST   /api/avaliacoes                           {produtoId, nota, descricao, comentario?}
DELETE /api/avaliacoes?produtoId=N               apaga a própria
DELETE /api/avaliacoes?produtoId=N&usuarioId=M   admin apaga a de outro
```

`nota` é um inteiro de 1 a 5 (400 se estiver fora da faixa). Avaliar de novo o
mesmo produto **atualiza** a avaliação existente em vez de criar outra
(`ON CONFLICT (usuario_id, produto_id) DO UPDATE`). Apagar é exclusão lógica
(`ativo = FALSE`).

`GET /api/produtos` e `GET /api/produtos/{id}` já devolvem `notaMedia` e
`totalAvaliacoes`, que vêm da view `produto_nota_media` do schema canônico —
`notaMedia` é `null` enquanto o produto não tiver nenhuma avaliação ativa.

### Cupons
```
GET    /api/cupons
GET    /api/cupons?codigo=BEMVINDO10
GET    /api/cupons/{id}
POST   /api/cupons                        admin  {codigo, tipoDesconto, valor, validade, ativo?}
PUT    /api/cupons/{id}                   admin
DELETE /api/cupons/{id}                   admin
```
`tipoDesconto` aceita `PERCENTUAL` (valor = % sobre o subtotal) ou `VALOR_FIXO`
(valor em reais, limitado ao subtotal).

## Regras do checkout

Tudo acontece numa única transação JDBC: se qualquer passo falhar, nada é
gravado (nem estoque, nem pedido).

1. O carrinho não pode estar vazio.
2. O endereço precisa pertencer ao usuário logado.
3. **Frete**: fixo de R$ 15,00, grátis a partir de R$ 80,00 de subtotal.
4. **Cupom** (opcional): precisa existir, estar ativo, não estar vencido e não
   ter sido usado antes pelo mesmo usuário.
5. **Estoque**: baixado com `UPDATE ... WHERE quantidade_estoque >= ?`, então a
   checagem e o decremento são atômicos e duas compras simultâneas não deixam o
   estoque negativo. Se faltar, a transação inteira é desfeita.
6. **Pagamento**: registro criado já com status `APROVADO`.
7. O carrinho é esvaziado.

Status do pedido: `PAGO`. Status do pagamento: `APROVADO`. São os valores que o
schema canônico usa (a coluna `status` é texto livre, sem `CHECK`).

### O que é simulado

Não há integração com gateway de pagamento, adquirente ou antifraude — nenhum
dado de cartão passa pela API. O registro em `pagamento` é criado direto como
aprovado, só para o fluxo ter um fim. O frete também é um valor fixo, sem
cálculo real de transportadora.

Trocar isso por uma cobrança de verdade significa substituir apenas o trecho
marcado em `service/CheckoutService.java`.

## Estrutura

```
src/main/webapp/WEB-INF/web.xml  ordem dos filtros e configuração da sessão
src/main/java/com/fysikos/
  model/       POJOs das entidades
  dao/         ConnectionFactory (JDBC) e um DAO por tabela
  service/     CheckoutService (a transação de checkout)
  controller/  servlets (@WebServlet) e Resposta (JSON de resposta/erro)
  filter/      CorsFilter e AutenticacaoFilter
  util/        PasswordUtil (BCrypt), CorpoJson (validação), Sessao, GsonFactory
```

Não há `db/` nem `docker-compose.yml` aqui: o banco é responsabilidade do
`ecommerce-data`. O `ConnectionFactory` só lê `DB_URL`, `DB_USER` e `DB_PASSWORD`
e se conecta ao banco que já existe.

Os DAOs têm dois estilos de método: os que abrem a própria conexão e os que
recebem uma `Connection` e propagam `SQLException`. Os segundos existem para o
`CheckoutService` compor várias operações na mesma transação.

## Notas

- **Senhas usam BCrypt.** O seed do `ecommerce-data` já grava hashes BCrypt
  (cost 12), gerados com o mesmo `jbcrypt` que a API usa. Se aparecer no banco
  algum usuário com hash de outro formato (SHA-256, texto puro), ele não
  consegue logar — o hash precisa ser regerado.
- **Excluir conta é exclusão lógica** (`usuario.ativo = FALSE`), não `DELETE`. A
  linha fica porque `avaliacao`, `pedido` e `uso_cupom` apontam para
  `usuario(id)` sem `ON DELETE CASCADE` — apagar de verdade quebraria a foreign
  key e levaria junto o histórico de pedidos. (`endereco` e `carrinho` têm
  cascade, mas os outros três não.) A coluna `usuario.ativo` faz parte do
  `db/01-schema.sql` do `ecommerce-data`, na mesma convenção que `produto`,
  `cupom` e `avaliacao` já usavam.
- O `GsonFactory` registra adaptadores para `LocalDate`/`LocalDateTime` (senão as
  datas saem como objeto interno do Java) e serializadores para `Carrinho`,
  `ItemCarrinho` e `ItemPedido`, porque `total` e `subtotal` são calculados e o
  Gson só serializa campos, não getters.
- `Usuario.senhaHash` é `transient`, então o hash nunca aparece numa resposta.
- Em `GET /api/pedidos` (lista) o campo `pagamento` vem `null`; o detalhe
  (`GET /api/pedidos/{id}`) traz o pagamento completo.
