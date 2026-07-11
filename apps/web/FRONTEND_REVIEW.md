# Revisão do front end (`apps/web`)

## Contexto observado

- O front end está concentrado em `src/app/page.tsx`, que hoje combina autenticação, persistência de sessão, chamadas HTTP, renderização das áreas de participante e organizador, formulários, helpers de data/status e tipos de contrato com a API em um único arquivo.
- A aplicação usa App Router do Next.js, mas a navegação funcional ainda é controlada por estado local dentro da home: cadastro, login, restauração de sessão, área do participante e área do organizador aparecem no mesmo fluxo de renderização.
- O bloqueio inicial de rotas pode ser antecipado no `src/proxy.ts` por cookie, evitando flash de conteúdo protegido; a validação real da sessão deve continuar no layout protegido e a autorização definitiva das ações permanece no backend.
- A documentação de `apps/web/README.md` ainda está no texto padrão do `create-next-app`, enquanto a implementação já depende de `NEXT_PUBLIC_API_URL`, endpoints da API e perfis de usuário do Event Manager.

## Tarefa 1 — Desacoplar `page.tsx` em camadas menores

**Objetivo:** reduzir o risco de regressão e facilitar evolução/testes separando UI, contratos e acesso à API.

**Proposta de escopo:**

1. Criar uma pasta `src/features/auth` com:
   - componentes `AuthCard`, `CadastroForm`, `LoginForm` e `SessionRestorer`;
   - hook `useSessao` para restaurar, salvar e encerrar sessão;
   - client `authApi` para `/autenticacao/cadastro`, `/autenticacao/login` e `/usuarios/atual`.
2. Criar uma pasta `src/features/eventos` com:
   - componentes da área do organizador (`AreaOrganizador`, formulário de evento, lista de eventos e inscritos);
   - client `eventosApi` para `/eventos` e `/eventos/{id}/inscricoes`.
3. Criar uma pasta `src/features/catalogo` ou `src/features/inscricoes` com:
   - componentes da área do participante, catálogo, detalhes e histórico;
   - client para `/catalogo/eventos` e `/inscricoes`.
4. Mover tipos compartilhados para `src/types/api.ts` e helpers de formatação para `src/lib/formatters.ts`.
5. Manter `src/app/page.tsx` como uma composição pequena da landing/autenticação ou redirecionamento inicial.

**Critérios de aceite sugeridos:**

- `src/app/page.tsx` fica responsável apenas por compor a tela inicial e delegar comportamentos.
- Nenhum componente de formulário faz `fetch` diretamente; chamadas passam por clients/hooks.
- Tipos de `Sessao`, `Evento`, `EventoCatalogo` e `Inscricao` deixam de ficar declarados na página.
- `pnpm --filter web lint` continua passando.

## Tarefa 2 — Ajustar roteamento com autenticação e rotas protegidas

**Objetivo:** transformar o estado autenticado em navegação explícita, preparando rotas protegidas por perfil.

**Proposta de escopo:**

1. Criar `src/proxy.ts` para fazer o bloqueio inicial por cookie antes da renderização:
   - bloquear `/organizador` e `/participante` quando não houver cookie de sessão;
   - redirecionar para a tela pública de login/cadastro;
   - evitar flash de conteúdo protegido durante a navegação inicial.
2. Criar grupos de rota no App Router:
   - `src/app/(public)/page.tsx` para landing, cadastro e login;
   - `src/app/(protected)/layout.tsx` para validar a sessão real consultando a API antes de renderizar áreas autenticadas;
   - `src/app/(protected)/participante/page.tsx` para catálogo e inscrições;
   - `src/app/(protected)/organizador/page.tsx` para eventos próprios e inscritos.
3. Criar layouts por perfil:
   - `src/app/(protected)/organizador/layout.tsx` valida role `ORGANIZADOR` e aplica layout próprio da área de gestão;
   - `src/app/(protected)/participante/layout.tsx` valida role `PARTICIPANTE` e aplica layout próprio da área de catálogo/inscrições.
4. Centralizar a sessão em um `AuthProvider`/`useSessao` no cliente ou evoluir para cookie HTTP-only caso a API passe a emitir token em cookie. O proxy deve fazer apenas o bloqueio preliminar; a sessão real deve ser validada no layout protegido.
5. Implementar redirecionamentos por perfil após login:
   - `PARTICIPANTE` → `/participante`;
   - `ORGANIZADOR` → `/organizador`.
6. Proteger acesso cruzado por perfil:
   - participante tentando `/organizador` deve ir para `/participante` ou ver página 403;
   - organizador tentando `/participante` deve ir para `/organizador` ou ver página 403.
7. Manter a autorização definitiva das ações no backend, que já é a fonte de verdade para permissões e regras de negócio.

**Critérios de aceite sugeridos:**

- Usuário sem cookie de sessão é bloqueado no `src/proxy.ts` antes de renderizar `/participante` ou `/organizador`.
- Usuário com cookie inválido é barrado no `src/app/(protected)/layout.tsx`, tem credenciais removidas e retorna para login.
- Login redireciona para a rota correta conforme `usuario.perfil`.
- Layouts de participante e organizador validam suas roles antes de renderizar o conteúdo de cada área.
- Áreas de participante e organizador têm URLs dedicadas e podem evoluir independentemente.
- Chamadas de escrita continuam dependendo da autorização do backend, não apenas da proteção visual/roteamento no front end.

## Tarefa 3 — Corrigir discrepância de documentação

**Objetivo:** alinhar a documentação do web app ao estado real do produto.

**Discrepância identificada:** `apps/web/README.md` ainda descreve um projeto genérico criado com `create-next-app`, menciona edição de `app/page.tsx` e não documenta as dependências reais do Event Manager, como API esperada, variável `NEXT_PUBLIC_API_URL`, perfis suportados e fluxos implementados.

**Proposta de escopo:**

1. Reescrever `apps/web/README.md` com:
   - visão geral do front end do Event Manager;
   - pré-requisitos e comandos com pnpm;
   - variável `NEXT_PUBLIC_API_URL` e valor padrão usado localmente;
   - fluxos disponíveis: cadastro/login, participante, organizador;
   - endpoints consumidos pela UI;
   - limitações conhecidas, incluindo concentração atual em `src/app/page.tsx` enquanto a refatoração não acontece.
2. Conferir se o README raiz referencia os mesmos comandos e portas para web/API.
3. Se houver mudança futura em endpoints ou payloads, atualizar README e coleção do Insomnia na mesma tarefa para evitar divergência entre documentação e contrato executável.

**Critérios de aceite sugeridos:**

- `apps/web/README.md` não contém mais texto padrão do template.
- Uma pessoa nova consegue subir o web app localmente com a API usando apenas a documentação.
- A documentação lista explicitamente o valor padrão de API (`http://localhost:8080`) e como sobrescrevê-lo.
