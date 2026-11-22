# Ficha técnica do sistema legado (Etapa 1)

## Identificação

| Item | Descrição |
|---|---|
| Nome | Lift Pass Pricing (API de preços de passe de teleférico) |
| Origem | [Refactoring-Kata-Lift-Pass-Pricing](https://github.com/martinsson/Refactoring-Kata-Lift-Pass-Pricing), de Johan Martinsson; porte Java de Peter Kofler (2019) |
| Tipo | API REST (back-end, sem interface gráfica) |
| Parte usada | pasta `java/` e o script `database/initDatabase.sql` |
| Tamanho | 2 classes de produção (158 linhas), 1 classe de teste |

O sistema calcula o preço de um passe de teleférico de uma estação de esqui. O preço depende do tipo de passe (diurno ou noturno), da idade do cliente e do dia em que ele vai esquiar, considerando segundas-feiras e feriados. Também permite cadastrar ou alterar o preço base de um tipo de passe.

## Tecnologias legadas

| Tecnologia | Versão no projeto | Situação |
|---|---|---|
| Java | 8 (`maven.compiler.source 1.8`) | Lançado em 2014. Três versões LTS mais novas (11, 17 e 21) já estão disponíveis. |
| Spark Java (`spark-core`) | 2.7.2 (março de 2018) | A última versão é a 2.9.4, de julho de 2022. O projeto não teve mais lançamentos depois disso. |
| `mysql-connector-java` | 8.0.28 (dezembro de 2021) | O artefato foi renomeado para `com.mysql:mysql-connector-j` a partir da 8.0.31. O nome antigo não recebe mais versões. |
| MySQL | servidor externo em `localhost:3306` | Usuário `root` e senha `mysql` fixos no código. |
| slf4j-simple | 1.7.13 (2015) | A linha 1.7 terminou na 1.7.36. |
| JUnit | 5.4.1 (2019) | |
| REST Assured | 3.3.0 (2019) | Usado só no teste de exemplo. |
| jackson-databind | 2.9.10.8 (teste) | A linha 2.9 não recebe mais correções. |
| APIs de data | `java.util.Date`, `Calendar`, `SimpleDateFormat` | Substituídas pelo `java.time` desde o Java 8. Os métodos `getYear()`, `getMonth()` e `getDate()` de `Date` estão depreciados. |

Para rodar o sistema como ele veio é preciso instalar um MySQL (ou subir um contêiner Docker), criar o usuário `root` com a senha `mysql` e executar o script do banco. Sem isso nem a suíte de testes roda: `mvn verify` falha com `Communications link failure`.

## Funcionalidades

| Rota | Parâmetros | O que faz | Onde está no código |
|---|---|---|---|
| `GET /prices` | `type` (`1jour` ou `night`), `age` (opcional), `date` no formato `yyyy-MM-dd` (opcional) | Devolve `{ "cost": N }` com o preço do passe | lambda `get("/prices", ...)` em `Prices.createApp()`, linhas 42 a 121 |
| `PUT /prices` | `type`, `cost` | Cadastra um tipo de passe ou atualiza o preço base de um tipo existente | lambda `put("/prices", ...)` em `Prices.createApp()`, linhas 26 a 40 |
| inicialização | nenhum | Abre a conexão com o MySQL e sobe o servidor na porta 4567 | `Main.main()` e começo de `Prices.createApp()` |

Os dados ficam em duas tabelas: `base_price` (preço base por tipo: `1jour` = 35 e `night` = 19) e `holidays` (feriados: 18/02/2019, 25/02/2019 e 04/03/2019).

## Regra de preço

A regra não está escrita em nenhum lugar além do código. A tabela de decisão abaixo foi montada lendo a lambda do `GET /prices` e depois conferida contra a aplicação rodando.

| Tipo | Idade | Segunda-feira que não é feriado | Outros dias | Exemplo (base 35 ou 19) |
|---|---|---|---|---|
| qualquer | menor que 6 | grátis | grátis | 0 |
| `1jour` | 6 a 14 | 70% do preço (sem desconto de segunda) | 70% do preço | 25 |
| `1jour` | 15 a 64 | 65% do preço (desconto de 35%) | preço cheio | 23 / 35 |
| `1jour` | maior que 64 | 75% × 65% do preço | 75% do preço | 18 / 27 |
| `1jour` | não informada | 65% do preço | preço cheio | 23 / 35 |
| `night` | 6 a 64 | preço cheio | preço cheio | 19 |
| `night` | maior que 64 | 40% do preço | 40% do preço | 8 |
| `night` | não informada | grátis | grátis | 0 |

Todos os valores são arredondados para cima (`Math.ceil`). Qualquer tipo diferente de `night` que exista na tabela `base_price` segue a regra do `1jour`, então um tipo novo cadastrado pelo `PUT` é tratado como passe diurno.

Dois comportamentos chamam a atenção. O primeiro é que crianças de 6 a 14 anos não recebem o desconto de segunda-feira, enquanto adultos e idosos recebem. O código tem um `// TODO apply reduction for others` exatamente nesse ponto, o que indica uma regra que ficou pela metade. O segundo é que o passe noturno sem idade informada sai de graça. Os dois foram mantidos na refatoração, porque mudar regra de negócio não é papel da equipe técnica sem uma decisão do dono do produto.

## Justificativa da escolha

Martins, Chervenski e Bordin (2017) apontam como características recorrentes de sistemas legados a tecnologia obsoleta, a falta de documentação, a dificuldade de manutenção e o alto acoplamento. O Lift Pass Pricing tem todas elas em um tamanho que cabe no prazo da disciplina: roda sobre Java 8 e um framework web sem lançamentos desde 2022, depende de um banco configurado de um jeito específico, não tem documentação da regra de negócio e concentra HTTP, SQL e regra de preço num único método de 80 linhas.

O tamanho pequeno também foi um critério. Com menos de 200 linhas, a equipe consegue entender o sistema inteiro, cobri-lo com testes e aplicar uma refatoração completa, em vez de mexer em uma parte pequena de um sistema grande. O próprio kata pede uma funcionalidade nova (cotar vários passes de uma vez) que o desenho atual não permite reaproveitar, o que dá um motivo de negócio concreto para a refatoração.

## Problemas encontrados (lista preliminar)

1. Toda a lógica do `GET /prices` (HTTP, SQL e regra de preço) está numa única lambda de 80 linhas.
2. Condicionais aninhadas em vários níveis, com a checagem de idade repetida nos ramos diurno e noturno.
3. A montagem da resposta `"{ \"cost\": " + ... + "}"` aparece 8 vezes, e o JSON é feito por concatenação de texto.
4. Números mágicos: 6, 15, 64, 35, 0.7, 0.75, 0.4, `DAY_OF_WEEK == 2` e `"night"`.
5. URL, usuário e senha do banco e a porta fixos no código.
6. Uma única conexão JDBC global, compartilhada por todas as requisições.
7. APIs de data depreciadas. A data da requisição é convertida de novo a cada feriado lido, e todos os feriados são carregados para verificar um único dia.
8. Entradas inválidas (sem `type`, tipo inexistente, idade ou data mal formatadas, `cost` não numérico) derrubam a requisição com erro 500.
9. Regra incompleta marcada com `TODO`.
10. O único teste é um exemplo que verifica `"putSomethingHere"`. Não há cobertura real.
11. Não existe documentação da regra de negócio.
