# Contratos da API usados pelo Android

Fonte atual: chamadas do frontend web em `api.js`, `bootstrap.js` e `app.js`.
O backend nao esta neste repositorio, portanto estes contratos devem ser confirmados em homologacao.

## Autenticacao

- `GET /health`: verifica disponibilidade sem autenticacao.
- `POST /api/login`: recebe `username`, `password` e `otp` opcional; retorna `api_key` e dados do usuario.
- `GET /api/me`: valida `x-api-key` e retorna o perfil atual.
- Todas as demais chamadas usam `x-api-key`; o frontend nao implementa refresh token.
- `401` significa chave invalida ou expirada e exige novo login.
- `403` significa operacao sem permissao e nao deve desconectar a conta automaticamente.

## Nucleo financeiro

- `GET /api/transactions?limit=1000`
- `POST /api/transactions`
- `PUT /api/transactions/{id}`
- `DELETE /api/transactions/{id}`
- Campos observados: `type`, `description`, `amount`, `category`, `date`, `purchase_date`, `note`, `account_id`, `paid`, `pending`, `installment_group`, `installment_num`, `installment_total`, `recur_group` e `split_meta`.

## Estado agregado

- `GET /api/state`
- `PUT /api/state`
- Blocos observados: `accounts`, `categories`, `shopping`, `car` e `settings`.
- O Android clona o estado recebido e preserva inclusive blocos desconhecidos antes de executar `PUT`.
- Contas alteram apenas `accounts`; vencimentos alteram apenas os campos Android dentro de `settings`.

## Implementacao Android

- `FinanzaApiRoutes` e a fonte unica das rotas do nucleo.
- `FinanzaRemoteRepository` concentra snapshot, transacoes e estado agregado.
- A interface `FinanzaJsonTransport` permite validar metodo, rota e preservacao do payload sem acessar uma conta real.

## Planejamento

- `GET|POST /api/budgets`, `DELETE /api/budgets/{id}`
- `GET|POST /api/goals`, `DELETE /api/goals/{id}`
- `PATCH /api/goals/{id}/add`
- Como a API observada nao oferece `PUT` para orcamentos ou metas, edicoes remotas substituem o registro com `DELETE` seguido de `POST`.

## Importacao e backup

- `PUT /api/import`: recebe o backup completo usado pelo web.
- O Android aceita backup JSON, CSV delimitado por virgula ou ponto e virgula e OFX/QFX.
- CSV e OFX passam por previa, deduplicacao exata e escolha entre manter o atual ou usar o importado em conflitos.
- Importacoes feitas offline permanecem locais e uma unica versao atualizada do backup fica na fila para envio.

## Modulos no estado agregado

- `shopping.lists` e `shopping.items` seguem os aliases `icon|ico`, `list_id|listId`, `category|cat` e `created_ms|createdAt`.
- `car.vehicles`, `car.events` e `active_vehicle_id|activeVehicleId` seguem o contrato normalizado do web.
- `settings.commitments` contem `subscriptions`, `debts` e `contracts`, aceitando campos em snake_case ou camelCase.
- Atualizacoes desses modulos enviam o estado agregado completo preservando chaves desconhecidas.

## Regra offline

- Falha de rede mantem dados locais e enfileira a escrita.
- `401` mantem dados e fila, mas bloqueia novos envios ate novo login.
- Uma sincronizacao completa so limpa o erro depois de esvaziar as filas e concluir todas as leituras.

## Compartilhado

- O servidor canoniza o espaco compartilhado em `settings.rates.sharedSpace|shared_space`; o Android tambem le o alias direto legado e sempre envia o caminho canonico junto do estado agregado.
- Pessoas usam `id`, `name`, `color` e `permission`; papeis aceitos no Android: `owner`, `editor`, `read` e `guest`.
- Convites usam payload versao 1 com `mode`, `name`, `ownerPersonId` e `people`, dentro de `?invite=<base64>`.
- O Android aceita links HTTPS do Finanza web e `finanza://invite`, mescla nomes sem duplicar e preserva o responsavel local.
- Gastos divididos usam `split_meta.kind=equal`, `payerId`, `participants` e `approval.status`.
- Apenas aprovacoes ausentes ou `approved` entram no saldo; `pending` e `rejected` ficam fora do calculo.
- Acertos sao transacoes comuns com `split_meta.kind=settlement`, `fromPersonId` e `toPersonId`.
- Aprovar ou recusar atualiza a transacao por `PUT /api/transactions/{id}`.

## Conta, seguranca e administracao

- `GET /api/me`: atualiza nome, id, papel e estado do 2FA da sessao atual.
- `POST /api/me/recovery-code`: gera codigo de recuperacao.
- `POST /api/me/2fa/setup`: gera segredo ou URI para o autenticador.
- `POST /api/me/2fa/confirm`: confirma codigo de seis digitos.
- `DELETE /api/me/2fa`: desativa 2FA mediante codigo atual.
- `POST /api/password-reset/recovery`: redefine senha com usuario, nova senha e codigo de recuperacao.
- `GET|POST /api/users`, `PATCH /api/users/{id}/role` e `DELETE /api/users/{id}`: administracao de usuarios.
- Papeis observados no web: `admin`, `editor`, `read` e `guest`; somente `admin` administra usuarios e `read|guest` nao escrevem dados.
- `GET /api/admin/overview` e `GET /api/audit-log?limit=30`: diagnostico e auditoria do ambiente.
- O Android impede alterar o papel ou excluir a propria conta antes de chamar a API.
- A API observada nao expoe uma rota para listar ou revogar outras sessoes. O Android mostra e encerra a sessao deste aparelho, sem simular uma lista inexistente.
