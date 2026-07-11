# Collection Insomnia — Event Manager POS

## Objetivo

Esta collection representa a superfície HTTP implementada atualmente em `apps/api`. A fonte principal foi o código dos controllers e da configuração de segurança; DTOs, validators, services, handlers de erro e testes MVC foram usados para completar contratos, regras e exemplos.

Arquivo para importação: [`insomnia-collection.json`](./insomnia-collection.json).

## Importação

1. Abra o Insomnia.
2. Selecione **Import** e depois **File**.
3. Escolha `docs/insomnia/insomnia-collection.json`.
4. Abra a collection **API - Event Manager POS**.
5. Selecione o subambiente **Local**, **Homologacao** ou **Producao**.
6. Preencha as credenciais e URLs marcadas com `PREENCHER_...` antes de executar requests que dependam delas.

O arquivo usa o formato de exportação Insomnia JSON v4. Todas as URLs de request são compostas como `{{ base_url }}{{ api_prefix }}/...`; nenhuma request contém host fixo.

## Environments

O Base Environment contém as variáveis compartilhadas:

- Endereço: `base_url` e `api_prefix`.
- Tokens: `access_token`, `organizer_access_token`, `participant_access_token` e `refresh_token`.
- Usuários: `user_id`, `organizer_id` e `participant_id`.
- Recursos: `evento_id`, `inscricao_id` e `id_inexistente`.
- Credenciais de execução: `organizer_email`, `organizer_password`, `participant_email`, `participant_password`, `new_organizer_email` e `new_organizer_password`.
- Dados auxiliares: `participant_name`, `new_organizer_name` e `imagem_url`.
- Compatibilidade futura: `page` e `page_size`; nenhum endpoint atual consome essas variáveis.

Subambientes:

- **Local:** `http://localhost:8080`. A API não define `server.port`, portanto usa a porta padrão do Spring Boot; o frontend também usa essa URL como fallback.
- **Homologacao:** `PREENCHER_URL_DE_HOMOLOGACAO`.
- **Producao:** `PREENCHER_URL_DE_PRODUCAO`.

Não foram encontradas URLs de homologação ou produção no repositório. `api_prefix` permanece vazio porque não há context path nem versionamento global configurado.

## Credenciais e autenticação

A API usa JWT Bearer assinado com HS256. O token contém `sub` com o ID do usuário e `perfil` com `ORGANIZADOR` ou `PARTICIPANTE`. O backend valida também issuer, audience `event-manager-api` e expiração.

Nenhuma senha, token, segredo JWT ou connection string real foi incluído. Preencha somente no environment local do Insomnia:

- `organizer_email` e `organizer_password` para um organizador já existente;
- `participant_email` e `participant_password` para um participante existente ou cadastrado pela collection;
- `new_organizer_*` apenas se for testar o cadastro administrativo.

Execute **01 - Autenticacao / Login como organizador** ou **Login como participante**. O script after-response acessa diretamente o contrato real `tokenAcesso` e `usuario.id`, valida a resposta e grava:

- sempre: `access_token` e `user_id`;
- organizador: `organizer_access_token` e `organizer_id`;
- participante: `participant_access_token` e `participant_id`.

Não existe refresh token implementado; `refresh_token` fica vazio. Também não existem endpoints de refresh ou logout.

As pastas protegidas herdam Bearer Token da pasta pai:

- pastas de organizador: `{{ organizer_access_token }}`;
- pastas de participante: `{{ participant_access_token }}`;
- endpoints válidos para ambos os perfis e Infra: `{{ access_token }}`.

## Captura automática de IDs

Os scripts after-response salvam apenas identificadores reutilizados:

- cadastro de participante → `participant_id`;
- login → ID e token do perfil correspondente;
- cadastro administrativo de organizador → `organizer_id`;
- criação de evento → `evento_id`;
- criação de inscrição → `inscricao_id`.

## Fluxos

Na pasta `90 - Fluxos`, execute as requests na ordem numérica.

### Organizador

1. Login como organizador.
2. Criar evento e capturar `evento_id`.
3. Listar os próprios eventos.
4. Atualizar o evento.
5. Listar inscritos do evento.
6. Cancelar o evento.

### Participante

1. Login como participante.
2. Buscar `evento_id` no catálogo.
3. Criar inscrição e capturar `inscricao_id`.
4. Listar as próprias inscrições.
5. Cancelar a inscrição.
6. Reativar a inscrição.

O fluxo de participante requer um `evento_id` futuro, não cancelado e com vaga. Para testar os dois fluxos sobre o mesmo evento, faça as etapas 1–4 do organizador, execute todo o fluxo do participante e só então cancele o evento.

## Endpoints identificados

São 17 endpoints declarados em controllers e 1 endpoint de infraestrutura confirmado pelo Actuator.

| Domínio | Método | Rota | Autorização | Sucesso |
|---|---:|---|---|---:|
| Infra | GET | `/actuator/health` | JWT, qualquer perfil | 200 |
| Autenticação | POST | `/autenticacao/cadastro` | Público | 201 |
| Autenticação | POST | `/autenticacao/login` | Público | 200 |
| Usuários | GET | `/usuarios/atual` | JWT, qualquer perfil | 200 |
| Administração | POST | `/admin/organizadores` | ORGANIZADOR | 201 |
| Administração | GET | `/admin/usuarios` | ORGANIZADOR | 200 |
| Eventos | POST | `/eventos` | ORGANIZADOR | 201 |
| Eventos | GET | `/eventos` | ORGANIZADOR | 200 |
| Eventos | GET | `/eventos/{id}` | ORGANIZADOR e dono | 200 |
| Eventos | PUT | `/eventos/{id}` | ORGANIZADOR e dono | 200 |
| Eventos | DELETE | `/eventos/{id}` | ORGANIZADOR e dono | 200 |
| Catálogo | GET | `/catalogo/eventos` | PARTICIPANTE | 200 |
| Catálogo | GET | `/catalogo/eventos/{id}` | PARTICIPANTE | 200 |
| Inscrições | POST | `/inscricoes` | PARTICIPANTE | 201 ou 200 |
| Inscrições | GET | `/inscricoes` | PARTICIPANTE | 200 |
| Inscrições | DELETE | `/inscricoes/{id}` | PARTICIPANTE e dono | 200 |
| Inscrições | PATCH | `/inscricoes/{id}/reativar` | PARTICIPANTE e dono | 200 |
| Inscritos | GET | `/eventos/{eventoId}/inscricoes` | ORGANIZADOR e dono | 200 |

`GET /catalogo/eventos` aceita os query parameters opcionais `busca`, `categoria` e `ordem=asc|desc`. Listagens retornam arrays JSON sem envelope e sem paginação.

## Controllers analisados

- `AdminController`: cadastro de organizador e listagem de usuários.
- `AutenticacaoController`: cadastro público de participante e login.
- `CatalogoController`: listagem com busca/filtro/ordem e detalhes do catálogo.
- `EventoController`: criação, listagem, detalhe, atualização completa e cancelamento lógico.
- `InscricaoController`: criação, listagem, cancelamento e reativação.
- `InscritosEventoController`: inscritos de um evento do organizador.
- `UsuarioController`: usuário representado pelo JWT.

Também foram analisados `SegurancaConfig`, `ApiExceptionHandler`, `ErroSegurancaHandler`, DTOs, validação customizada de senha, enums, services, repositories, `application.properties`, `pom.xml` e testes dos controllers.

## Contrato de erros

Erros tratados usam `application/problem+json` (`ProblemDetail`) com `type`, `title`, `status`, `detail`, `instance` e `codigo`. Erros de validação acrescentam `erros`, um objeto por campo.

Códigos representados na pasta `99 - Casos de Erro`:

- `NAO_AUTENTICADO` — 401, token ausente/inválido ou usuário do token inexistente;
- `CREDENCIAIS_INVALIDAS` — 401 no login;
- `ACESSO_NEGADO` — 403 para perfil incompatível;
- `DADOS_INVALIDOS` — 400 para Bean Validation;
- `PERIODO_EVENTO_INVALIDO` — 400;
- `RECURSO_NAO_ENCONTRADO` — 404;
- `EMAIL_JA_CADASTRADO` — 409;
- `EVENTO_FINALIZADO` — 409;
- `INSCRICAO_NAO_PERMITIDA` — 409.

As descrições das requests registram payload, resposta principal e erros aplicáveis. Cada request também possui uma resposta de exemplo salva no arquivo. Os scripts validam status, conteúdo JSON e propriedades centrais; scripts de criação e login também capturam variáveis.

## Endpoints não representados integralmente

- O Springdoc está instalado e expõe `/v3/api-docs` e `/swagger-ui/index.html`. Esses endpoints auxiliares ficam públicos para facilitar inspeção e testes manuais da API.
- Apenas `/actuator/health` foi incluído em Infra. Readiness, liveness e versão da API não foram criados porque não há configuração que os exponha.
- `OPTIONS /**` é permitido para CORS, mas preflight não foi duplicado como request funcional.

## Pendências e inconsistências encontradas

- `DELETE /eventos/{id}` faz cancelamento lógico e devolve 200 com o evento. Esse contrato segue o PRD da base de autenticação/autorização, que definiu "Excluir Evento" como cancelamento lógico.
- O cadastro público não autentica automaticamente: retorna somente os dados públicos do participante. É necessário executar login depois.
- Não existem refresh token, logout, versionamento, paginação, profiles com URLs de ambiente, readiness, liveness ou endpoint de versão.
- Swagger/OpenAPI é gerado a partir dos controllers e inclui o esquema Bearer JWT para autorizar chamadas protegidas no Swagger UI. A documentação gerada não é a fonte usada para inferir regras de negócio.
