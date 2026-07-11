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
- CRUD/cancelamento lógico de eventos para organizadores.
- Listagem de eventos com status: futuro, ocorrendo e finalizado.
- Catálogo de eventos para participantes.
- Busca por texto, filtro por categoria e ordenação por data.
- Inscrição em eventos respeitando o limite de vagas.
- Cancelamento e reativação de inscrição.
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

Por padrão, o front-end sobe em `http://localhost:3000` e espera a API em
`http://localhost:8080`.

Execute apenas o front-end:

```bash
pnpm dev:web
```

Execute apenas a API:

```bash
pnpm dev:api
```

A API usa a porta padrão do Spring Boot (`8080`) quando `server.port` não é
sobrescrito. O front-end usa `NEXT_PUBLIC_API_URL` quando definida e cai para
`http://localhost:8080` quando a variável não existe.

Execute os testes da API:

```bash
pnpm --dir apps/api test
```

## Status atual

O produto segue em desenvolvimento, mas já possui API e web app integrados para
cadastro/login, área de participante, área de organizador, eventos, catálogo,
inscrições e lista de inscritos. A collection do Insomnia em
`docs/insomnia/insomnia-collection.json` documenta o contrato HTTP atual da API.
