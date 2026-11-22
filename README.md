# Lift Pass Pricing: evolução de um sistema legado

Trabalho da disciplina Evolução de Software (UCSAL, 2026.2), turma ESW-NOT-PIT-6S-T2, com a professora Sheila Tirony de Almeida Silva. O objetivo é analisar, documentar e refatorar um sistema legado real. O sistema escolhido é uma API REST que calcula o preço de passes de teleférico de uma estação de esqui.

## Créditos

O código de partida é o [Lift Pass Pricing Refactoring Kata](https://github.com/martinsson/Refactoring-Kata-Lift-Pass-Pricing), criado por Johan Martinsson, na versão em Java portada por Peter Kofler. Trouxemos para a raiz deste repositório a pasta `java/` do kata e o script `database/initDatabase.sql`.

Código original usado para fins acadêmicos; este repositório contém modificações.

## O que a API faz

- `GET /prices?type=1jour&age=30&date=2019-02-11` devolve o preço do passe, por exemplo `{ "cost": 23}`.
- `PUT /prices?type=1jour&cost=35` cadastra ou altera o preço base de um tipo de passe.

O preço depende do tipo de passe (`1jour` para o diurno, `night` para o noturno), da idade e da data. A regra completa está em [docs/01-ficha-tecnica.md](docs/01-ficha-tecnica.md).

## Como rodar

Só é preciso ter o JDK 21 e o Maven 3.9 ou mais novo. Não precisa instalar banco de dados.

```bash
mvn compile exec:java
```

Depois abra <http://localhost:4567/prices?type=1jour&age=30&date=2019-02-11> no navegador. Para parar, use Ctrl+C.

Para os testes:

```bash
mvn test
```

### Sobre o banco de dados

O kata original exigia um MySQL em `localhost:3306` com usuário `root` e senha `mysql`. Trocamos por um H2 em memória, que sobe junto com a aplicação e é criado a partir de `src/main/resources/db/init.sql` com os mesmos dados do script original. Os dados de exemplo voltam ao estado inicial a cada execução.

Para usar outro banco, passe a URL JDBC pela propriedade `liftpass.db.url` e adicione o driver correspondente no `pom.xml`:

```bash
mvn compile exec:java -Dliftpass.db.url="jdbc:mysql://localhost:3306/lift_pass?user=root&password=mysql"
```

Nesse caso o schema precisa ser criado antes com `database/initDatabase.sql`.

## Documentação

- [Ficha técnica e funcionalidades](docs/01-ficha-tecnica.md)

## Integrantes

| Integrante | Papel |
|---|---|
| Lucas de Almeida Borges | Etapa 1 e preparação da plataforma (Java 21, H2, CI) |
| Lucas Almeida dos Santos | Etapa 2 (engenharia reversa e débito técnico) e rede de testes |
| Murilo Carvalho Jesus de Oliveira | Etapa 3 (refatoração da regra e da API) e plano de evolução |
