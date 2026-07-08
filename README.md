# event-manager-pos

Monorepo da Atividade Final de Fullstack Developer da FACENS.

O objetivo do projeto é construir uma plataforma de gerenciamento de eventos,
com experiências separadas para organizadores e participantes.

## Stack

- Front-end: React com Next.js, TypeScript e Tailwind CSS
- Back-end: Spring Boot com Java 17
- Banco de dados: MongoDB
- Monorepo: pnpm workspaces

## Objetivo do sistema

A aplicação deve permitir que usuários se cadastrem e façam login com um dos
dois perfis:

- Organizador: cria, edita, exclui e acompanha seus próprios eventos.
- Participante: busca eventos, visualiza detalhes e gerencia suas inscrições.

O sistema deve controlar permissões por perfil, impedindo ações indevidas, como
um participante editar eventos ou um usuário alterar dados que não pertencem a
ele.

## Funcionalidades previstas

- Autenticação e autorização por perfil.
- CRUD de eventos para organizadores.
- Listagem de eventos com status: futuro, ocorrendo e finalizado.
- Catálogo de eventos para participantes.
- Busca por texto, filtro por categoria e ordenação por data.
- Inscrição em eventos respeitando o limite de vagas.
- Cancelamento de inscrição antes da data do evento.
- Área de "Minhas Inscrições" para participantes.
- Lista de inscritos por evento para organizadores.

## Estrutura

```text
apps/
  api/  # API Spring Boot
  web/  # Aplicação web Next.js
docs/
  requisitos.txt
```

## Comandos principais

Instale as dependências do workspace:

```bash
pnpm install
```

Execute front-end e back-end em modo desenvolvimento:

```bash
pnpm dev
```

Execute apenas o front-end:

```bash
pnpm dev:web
```

Execute apenas a API:

```bash
pnpm dev:api
```

Execute os testes da API:

```bash
pnpm --dir apps/api test
```

## Status atual

O repositório está em fase inicial. A API foi criada a partir do Spring
Initializer e a aplicação web foi criada com Next.js. A modelagem de domínio,
endpoints, autenticação, telas e integração com MongoDB ainda serão
implementadas.
