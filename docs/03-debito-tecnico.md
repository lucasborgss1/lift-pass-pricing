# Relatório de débito técnico (Etapa 2)

A análise foi feita sobre o código da tag `v0-original`. Os números de linha se referem a `Prices.java` nessa versão.

## Acoplamento

A lambda do `GET /prices` depende ao mesmo tempo do Spark (`req.queryParams`), do JDBC (`PreparedStatement`, `ResultSet`), do formato das tabelas, do formato da resposta e das classes de data. Qualquer mudança em uma dessas partes obriga a mexer no mesmo bloco de 80 linhas. Não há interfaces nem injeção de dependência: a conexão é criada dentro de `createApp()` com URL, usuário e senha fixos, e a porta 4567 também é fixa.

Dois acoplamentos merecem destaque:

- **Regra acoplada ao `ResultSet`.** O preço base é lido com `result.getInt("cost")` em seis pontos diferentes, dentro dos `if` de idade. A regra de preço só funciona com um cursor JDBC aberto.
- **Conexão global compartilhada.** Uma única `Connection` é criada na inicialização e usada por todas as requisições, que o Jetty atende em threads diferentes. O padrão JDBC não garante que uma conexão seja segura para uso concorrente, e se ela cair a aplicação inteira para de responder até ser reiniciada.

Esse acoplamento explica por que, no legado, só era possível escrever testes de ponta a ponta. Para executar a regra de preço é preciso uma requisição do Spark e um `ResultSet` de verdade, e o único jeito de ter os dois é subir o servidor HTTP e um MySQL configurado. Não existe nenhum método que receba tipo, idade e data e devolva um preço. Na terminologia de Feathers (2013), o código não tem costuras: não há ponto onde trocar o banco ou o HTTP por uma versão de teste sem editar o próprio método.

## Duplicação

| O que se repete | Ocorrências | Linhas |
|---|---|---|
| Montagem `"{ \"cost\": " + ... + "}"` | 8 | 56, 92, 96, 100, 103, 110, 112, 115 |
| `result.getInt("cost")` | 6 | 92, 95, 99, 102, 110, 112 |
| `(int) Math.ceil(...)` | 5 | 92, 96, 100, 103, 110 |
| `isoFormat.parse(req.queryParams("date"))` | 2, sendo uma dentro do laço | 70, 84 |
| Ramos idênticos: `cost * (1 - reduction / 100.0)` | 2 | 95-96 e 102-103 |
| Teste "idade menor que 6" | 2 | 55 e 108 (o segundo é sempre verdadeiro quando a idade existe) |

Os ramos das linhas 95-96 e 102-103 fazem exatamente a mesma conta: "sem idade" e "idade entre 15 e 64" são o mesmo caso, escrito duas vezes. Já a checagem `age >= 6` da linha 108 nunca é falsa quando `age` existe, porque a linha 55 já devolveu zero para os menores de 6.

## Complexidade ciclomática

A complexidade ciclomática de McCabe conta os caminhos independentes de um método: 1 mais o número de pontos de decisão. Contamos cada `if`, `else if`, `while`, `?:` e cada operador `&&` ou `||` dentro de uma condição. O `else` não conta, porque não cria um caminho novo.

| Linha | Construção | Pontos |
|---|---|---|
| 43 | `?:` da idade | 1 |
| 55 | `if (age != null && age < 6)` | 2 |
| 60 | `if (!type.equals("night"))` | 1 |
| 67 | `while (holidays.next())` | 1 |
| 69 | `if (date != null)` | 1 |
| 71 | `if (ano == ... && mês == ... && dia == ...)` | 3 |
| 82 | `if (date != null)` | 1 |
| 85 | `if (!isHoliday && DAY_OF_WEEK == 2)` | 2 |
| 91 | `if (age != null && age < 15)` | 2 |
| 94 | `if (age == null)` | 1 |
| 98 | `if (age > 64)` | 1 |
| 108 | `if (age != null && age >= 6)` | 2 |
| 109 | `if (age > 64)` | 1 |
| | **Total de pontos de decisão** | **19** |

Complexidade ciclomática da lambda do `GET /prices`: **19 + 1 = 20**. Valores acima de 10 costumam ser tratados como sinal de alerta, e acima de 20 o método já é considerado difícil de testar, porque seriam necessários pelo menos 20 casos de teste para passar por todos os caminhos independentes.

O aninhamento acompanha esse número. O `if` da linha 71 está dentro de outro `if`, de um `while`, de um `if` e de um `else`: são cinco níveis de estruturas de controle. Somando os quatro blocos `try` e a própria lambda, a linha 71 chega a 11 níveis de indentação.

## Falta de documentação

O README da pasta `java/` só explica como rodar o Maven. Não existe descrição da regra de preço, dos parâmetros aceitos, do formato da data nem das respostas de erro. O único comentário de negócio é `// TODO apply reduction for others`, que aponta um problema sem dizer qual é a regra correta. O teste existente é um modelo vazio (`"putSomethingHere"`), então também não serve como documentação executável. Para escrever a tabela de decisão do [doc 01](01-ficha-tecnica.md) foi preciso ler o código linha a linha e depois confirmar cada caso com a aplicação rodando.

## Baixa manutenibilidade

Juntando os pontos acima, uma mudança simples custa caro. Para incluir, por exemplo, um desconto de segunda-feira para crianças (o `TODO`), o desenvolvedor precisa entender a lambda inteira, achar os ramos certos entre oito `return`, e testar subindo servidor e banco. A funcionalidade pedida no kata, cotar vários passes numa única chamada, não reaproveita nada do que existe, porque a regra está presa à requisição HTTP.

A preparação do ambiente agrava o quadro. Sem MySQL instalado com usuário `root` e senha `mysql`, nem o `mvn verify` passa: o teste de exemplo falha com `Communications link failure`. Para quem chega no projeto, o primeiro contato já é uma configuração de banco.

## Code smells

Classificação segundo o catálogo de Fowler (2020).

| Smell | Onde | Severidade | Refatoração indicada |
|---|---|---|---|
| Long Function | lambda `GET /prices` (80 linhas) | Alta | Extract Function, Split Phase |
| Divergent Change | `Prices.createApp()` muda por motivo de HTTP, SQL ou regra | Alta | Extract Class, Move Function |
| Duplicated Code | JSON montado 8 vezes; ramos 95-96 e 102-103 iguais | Média | Extract Function, Consolidate Conditional Expression |
| Nested conditionals | linhas 55 a 117, cinco níveis | Alta | Replace Nested Conditional with Guard Clauses |
| Magic numbers | 6, 15, 64, 35, 0.7, 0.75, 0.4, `DAY_OF_WEEK == 2` | Média | Replace Magic Literal |
| Primitive Obsession | tipo de passe e data como `String` lidos direto do request | Baixa | Introduce Parameter Object (parcial) |
| Global Data | `Connection` única e porta fixa | Alta | Encapsulate Variable, injeção pela construção |
| Loop desnecessário | todos os feriados carregados e a data convertida de novo a cada volta | Média | Replace Loop with Pipeline (aqui, consulta com `WHERE`) |
| API obsoleta | `Date.getYear()`, `getMonth()`, `getDate()`, `Calendar`, `SimpleDateFormat` | Média | Substituir por `java.time.LocalDate` |
| Comentário de pendência | `// TODO apply reduction for others` | Baixa | Documentar a decisão pendente com o negócio |

## Entradas que derrubam a API

Todas as entradas abaixo foram testadas com a aplicação rodando. Em todos os casos a resposta é `500 Internal Server Error` com corpo HTML (mesmo a API dizendo que responde JSON), e o stack trace da exceção aparece no console do servidor.

| Requisição | Exceção |
|---|---|
| `GET /prices` (sem `type`) | `NullPointerException` em `req.queryParams("type").equals("night")` |
| `GET /prices?type=vip&age=30` (tipo inexistente) | `SQLException`, porque `result.next()` devolveu falso e `getInt` lê um cursor vazio |
| `GET /prices?type=1jour&age=abc` | `NumberFormatException` |
| `GET /prices?type=1jour&date=amanha` | `ParseException` |
| `PUT /prices?type=x&cost=abc` | `NumberFormatException` |
| `PUT /prices?type=x` (sem `cost`) | `NumberFormatException` |

Há ainda entradas inválidas que passam sem erro e dão respostas sem sentido. `type=vip&age=5` devolve `{ "cost": 0}` para um tipo que não existe. `age=-3` é aceito e cobra zero. `date=2019-02-30` é aceito porque o `SimpleDateFormat` é tolerante e transforma a data em 2 de março.

## Primeiro passo: deixar o legado executável

Antes de refatorar, era preciso conseguir rodar o legado e testá-lo sem MySQL. No commit `refactor: torna a conexão configurável e troca MySQL por H2 embutido` só a origem da conexão mudou: a URL fixa deu lugar à classe `Database`, que lê a propriedade `liftpass.db.url` e, por padrão, usa um H2 em memória em modo de compatibilidade com MySQL, criado a partir de `db/init.sql`. A lambda do `GET /prices` não mudou nenhum caractere. Essa troca é a primeira costura do sistema e é o que permite escrever os testes de caracterização sobre o comportamento original.
