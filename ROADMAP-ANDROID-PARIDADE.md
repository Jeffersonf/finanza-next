# Roadmap Android - Finanza Next

Objetivo atual: levar as funcionalidades do Finanza web para um app Android basico e rapido, usando a linguagem visual e de interacao do The Box no iOS.

## Referencias do produto

- Finanza web: fonte de funcionalidades, regras, dados e sincronizacao
- The Box no iOS: fonte do visual, navegacao, movimentos e captura rapida
- o Android nao copia a interface web e nao inventa regras ausentes no Finanza

## Atualizacao de arquitetura e entrega

O Android passa a ser **um unico app funcional com dois temas completos**. Next e Finanza nao sao mais tratados como aplicativos, bancos de dados ou navegacoes paralelas. Todo recurso entra uma vez e deve funcionar nos dois temas.

- **Dados, regras e fluxos:** Finanza web.
- **Tema Next:** linguagem atual, compacta e de alto contraste.
- **Tema Finanza:** aplicacao mobile direta da gramatica do Finanza web: fundo grafite em camadas, superficies translucidas, bordas discretas, lime como acao primaria, teal e roxo como sinais secundarios.

### Estado em 21/07/2026

- As Fases 1 a 6 estao presentes no codigo e usam a mesma base de dados, sessao e sincronizacao.
- A consolidacao visual e de desempenho e a entrega ativa: eliminar regressoes de navbar, modais e tema escuro antes de adicionar novas telas.
- A navbar agora inicia em **Inicio** e a troca de aba nao reconstrói o estado financeiro; clique troca a pagina imediatamente e o gesto lateral continua habilitado.
- Nesta rodada, a navbar foi unificada como controle The Box nos dois temas, o espaco residual sob ela foi removido e a imposicao de contraste das barras do sistema foi desativada.
- O tema especifico de Android 10+ preserva explicitamente `NoActionBar`; a barra nativa que exibia "Next" foi removida e a activity tambem a oculta defensivamente.
- `assembleDebug`, testes JVM e 34 testes instrumentados passaram no Xiaomi Android 15, incluindo troca por toque e gesto entre abas.
- O painel Finanza agora reproduz a cabeca do dashboard web: titulo, resumo, periodo e acao `Nova` na mesma composicao; a personalizacao de widgets deixou de ocupar uma faixa vazia.
- A Analise Finanza usa paineis de fluxo de caixa e distribuicao circular por categoria, em vez de uma lista extensa de barras; no tema claro, o acento e verde profundo e no escuro permanece lima, como no web.
- A Central Finanza organiza os modulos em uma grade compacta, preservando todas as entradas sem alongar a tela; rotulos e mensagens principais receberam revisao de acentuacao em portugues.
- Estados vazios dos modulos agora seguem a hierarquia dos cards Finanza: icone nativo, contexto curto e uma unica acao de cadastro; a composicao foi revisada no Xiaomi Android 15 e a bateria instrumentada passou com 38 testes.
- Os itens internos da Central Finanza receberam um layout proprio, mais proximo das linhas financeiras do web: icone, descricao, valor, status e acoes compactas, enquanto o tema Next preserva seus cards atuais.
- O editor dos modulos agora limita a folha a 66% da tela, rola somente os campos e fixa a confirmacao na base, evitando folhas extensas e a perda da acao de salvar.
- Rótulos financeiros, formulários e dados de exemplo receberam revisão de português; valores antigos conhecidos, como `Cartao`, são apresentados como `Cartão` sem alterar nomes livres do usuário.
- O dashboard Finanza não repete mais entradas, saídas e saldo dentro do mosaico de prioridades; a barra compartilhada The Box recebeu transparência real no escuro, preservando ordem, rótulos e auto-ocultação.

### Proximos marcos

1. **Estabilizacao visual:** validar Next e Finanza claro/escuro, barras do sistema, navbar, modais e acessibilidade de texto em aparelhos pequenos e grandes.
2. **Desempenho:** medir inicializacao, troca de abas, rolagem do dashboard e captura rapida em aparelho real; manter alteracoes de aba fora do caminho de recalculo e sincronizacao.
3. **Paridade web:** comparar cada fluxo do Android com o Finanza web autenticado, priorizando historico, contas/cartoes, vencimentos, planejamento, importacao e compartilhado.
4. **Publicacao:** testes em mais de um launcher, build release assinada, tamanho do APK, politica de privacidade e checklist da Play Store.

## Implementado no codigo atual

- inicio com resumo mensal, orcamento e ultimas transacoes
- cadastro e edicao de gastos, receitas e vencimentos
- contas, saldos e agenda financeira
- movimentos com filtros e busca
- login e sincronizacao basica com a API do Finanza
- backup local por compartilhamento de JSON
- tema claro/escuro neutro e privacidade de valores
- categorias canonicas com inferencia compartilhada entre app, captura rapida e widget
- icones nativos por categoria, conta e modulo, sem emojis na interface principal
- widget de resumo, widget de agenda e widget de captura rapida
- captura rapida em folha separada, atalho do launcher e notificacao inline
- navegacao inferior flutuante, semitransparente e com ocultacao automatica
- historico com busca, filtros e comparativo dos ultimos seis meses
- contas correntes, cartoes, dados de fechamento/vencimento e transferencias internas
- parcelas, recorrencias, observacoes e divisao igual de lancamentos
- orcamentos por categoria e metas com aportes
- assinaturas, dividas e contratos ligados aos vencimentos da agenda
- listas de compras com itens, quantidades e conclusao
- veiculos, abastecimentos, manutencoes, consumo e despesas no historico
- importacao JSON/CSV/OFX com revisao de duplicadas e backup completo
- fila offline para lancamentos e modulos, com reenvio na proxima sincronizacao
- fila offline de contas com substituicao da versao pendente e erro de sincronizacao visivel
- seguranca 2FA e administracao condicionada ao perfil online

## Estado atual

### Concluido e validado

- base Android em Compose com a linguagem visual do The Box
- tema escuro preto e papeis Material 3 completamente definidos, sem cores padrao vazando
- navegacao inferior flutuante, semitransparente e com ocultacao automatica
- captura rapida no fluxo valor -> descricao, teclado automatico e moeda em real
- persistencia local do nucleo financeiro e atualizacao dos widgets
- abertura do app, captura rapida e teclado validados em aparelho Android 15
- build debug e lint sem erros

### Fase 1 - concluida e homologada

- cliente HTTP unico para login, leitura e escrita na API do Finanza
- contrato da captura rapida alinhado ao `POST /api/transactions` usado pelo web
- captura pelo app e pelo widget enfileirada quando o servidor estiver indisponivel
- fila protegida contra perda concorrente e contra repeticao de operacoes ja enviadas
- alteracoes de contas preservadas em fila offline e status de erro/pendencias exposto em Ajustes
- login valida a chave retornada em `/api/me`; `401` exige novo login e `403` preserva a sessao
- escritas feitas com sessao expirada permanecem na fila ate a reconexao
- lancamentos Android enviam o mesmo `account_id` usado pelo web e operacoes locais pendentes sao compactadas para evitar duplicacao
- estado de vencimentos e estado de contas usam mutacoes unicas na fila offline
- contratos observados do frontend documentados em `android-kotlin/API-CONTRACTS.md`
- endpoint publico de saude validado com resposta multiusuario ativa
- rotas do nucleo centralizadas e operacoes remotas extraidas do `MainActivity` para `FinanzaRemoteRepository`
- snapshot financeiro, mutacoes de transacao e atualizacoes de estado cobertos por testes de contrato
- alteracoes de contas e vencimentos preservam modulos remotos desconhecidos pelo Android
- fila de modulos confirma operacoes individualmente e compacta apenas estados agregados substituiveis
- edicao de orcamento e meta remotos segue o contrato de substituicao observado no web
- historico, busca, filtros, contas, cartoes, transferencias e vencimentos possuem leitura, edicao, persistencia local, fila offline e estados de erro

O codigo e os contratos da Fase 1 foram validados com uma conta real: sessao, leitura, escrita, atualizacao, exclusao, estado agregado e sincronizacao no aparelho. Filas, conflitos e expiracao permanecem cobertos pela bateria instrumentada.

### Fase 2 - concluida no Android

- orcamentos ignoram gastos futuros ou ja liquidados, como no web
- metas, assinaturas, dividas e contratos usam os mesmos campos, status e aliases do frontend web
- comparativo mensal, entradas, saidas, evolucao, categorias, contas e progresso sao calculados a partir das transacoes persistidas
- regras de planejamento possuem testes JVM e instrumentados

### Fase 3 - concluida no Android

- JSON, CSV e OFX possuem parser separado, previa, deduplicacao e resolucao de conflitos
- importacao offline e enfileirada como backup completo substituivel
- listas e carro preservam listas/veiculos ao mesclar seus itens/eventos
- CSV aceita virgula ou ponto e virgula, campos entre aspas e formatos brasileiros; OFX/QFX normaliza identificadores estaveis

### Fase 4 - concluida no Android

- listas de compras possuem criacao, edicao, itens, quantidades, conclusao, exclusao protegida e persistencia
- despesas do carro ficam vinculadas a transacao financeira para exclusao consistente
- carro exibe consumo, custo por km, manutencao, proxima revisao e evolucao mensal
- veiculos, abastecimentos, hodometro, manutencoes e demais despesas possuem edicao, persistencia e normalizacao do estado web

As Fases 2 a 4 passaram por build, lint, testes JVM, testes instrumentados, revisao visual clara/escura e sincronizacao real no Xiaomi Android 15.

### Pendente para paridade com o Finanza web

1. Repetir a homologacao de permissoes usando logins separados `editor`, `read` e `guest`.
2. Validar widgets, notificacoes, icone e captura rapida em mais de um fabricante/launcher.
3. Criar build release assinada, revisar tamanho/desempenho e preparar publicacao.

### Fase 5 - concluida no Android

- espacos nos modos casal, familia e casa, com responsavel preservado
- pessoas com permissoes `owner`, `editor`, `read` e `guest`
- convites compativeis com o link web e deep link nativo, com confirmacao e mescla sem duplicar nomes
- divisao igual com escolha de pagador e participantes, alem de aprovacao opcional do gasto
- saldos calculados com as mesmas regras e sinais do frontend web
- aprovacao e recusa atualizam a transacao financeira; itens pendentes ou recusados nao alteram saldos
- acertos geram transacao vinculada, persistem localmente e usam a fila offline existente
- perfis de leitura visualizam o espaco, mas nao recebem controles de escrita

### Fase 6 - concluida no Android

- perfil local e atualizacao da identidade online por `/api/me`
- sessao atual identificada por usuario e servidor, com encerramento seguro neste aparelho
- troca de senha por codigo de recuperacao e exigencia minima de oito caracteres
- configuracao, confirmacao e desativacao de 2FA
- codigo de recuperacao e renovacao dos dados da sessao
- privacidade de valores preservada nas telas financeiras e compartilhadas
- painel administrativo condicionado ao papel autorizado, com diagnostico, auditoria e usuarios
- criacao de usuario, quatro papeis do web, alteracao de permissao e exclusao com protecao da propria conta

A API observada nao possui rota para listar ou revogar outras sessoes conectadas. O Android implementa a sessao deste aparelho e documenta essa limitacao sem apresentar dados ficticios.

### Homologacao online das Fases 5 e 6

- conta `admin` real validada por `/api/me`
- estado compartilhado gravado e relido no caminho canonico `settings.rates.sharedSpace`, com restauracao do estado original
- transacao dividida criada, aprovada e excluida pela API
- usuario temporario criado como `guest`, alterado para `read` e excluido
- painel, auditoria e listagem de usuarios consultados com sucesso
- nenhum usuario ou transacao temporaria permaneceu no servidor
- 2FA e senha nao foram alterados durante a homologacao para preservar o acesso da conta

## Regra de produto

O menu do Android deve mostrar apenas fluxos funcionais. Modulos futuros nao aparecem como se estivessem prontos; entram no app quando tiverem dados, edicao, persistencia e validacao completas.

## Validacao

- APK final instalada e abertura principal validadas em Xiaomi Android 15
- captura visual posterior a instalacao confirma que nao ha ActionBar nativa nem titulo "Next" acima da interface Compose
- o tema Finanza usa Syne nos titulos e DM Sans no conteudo, carregadas localmente e isoladas do tema Next
- o cabecalho do dashboard Finanza concentra periodo, criacao e personalizacao; a faixa redundante "Widgets do painel" foi removida para espelhar a estrutura mobile do web
- o resumo do dashboard Finanza usa a grade mobile de quatro indicadores do web (saldo total, receitas, despesas e a pagar), enquanto o cartao compacto original permanece exclusivo do Next
- dashboard, contas e analise do tema Finanza escuro revisados em Xiaomi Android 15; a barra inferior continua sendo o controle flutuante The Box compartilhado pelos dois temas
- a tela Contas do tema Finanza agora usa a grade compacta de carteiras do web, com icone, etiqueta de tipo, saldo e abertura do editor; o Next preserva sua lista original
- Ajustes passa a usar o cabeçalho do Finanza web ("Configurações" e "App e preferências") somente nesse tema, sem alterar os grupos e controles do Next
- os tokens claros do Finanza foram alinhados ao azul-gelo do web (`#F0F4FF`) e os estados de erro passaram a usar o coral Finanza nos modos claro e escuro, sem herdar a paleta iOS do Next
- as transacoes do tema Finanza exibem a categoria como badge colorida, reproduzindo a hierarquia visual do historico web; o Next mantem a leitura compacta em texto
- analise Finanza clara e escura revisada no Xiaomi, incluindo legenda, barras e distribuicao por categoria; a preferencia final do aparelho voltou para Finanza escuro
- login renderizado e verificado nos dois temas e nos modos claro/escuro, preservando entrada online e o modo local; a bateria instrumentada conta com 38 testes no Xiaomi
- quatro aberturas frias consecutivas no Xiaomi ficaram entre 1,32 s e 1,40 s; a navegacao por toque e gesto e coberta pela bateria instrumentada
- o pager permanece com composicao sob demanda: a tentativa de pre-carregar a pagina vizinha elevou a abertura a frio para 1,45-1,48 s, enquanto a configuracao final voltou a 1,30-1,34 s nas execucoes estaveis
- auditoria posterior das telas Contas e Analise no Xiaomi removeu a repeticao de tipo nas contas (por exemplo, `Cartao`/`Cartao`), preservando os dados e exibindo contexto financeiro; tres novas aberturas frias ficaram entre 1,32 s e 1,34 s
- captura rapida validada por teste instrumentado no aparelho: valor -> descricao, sem etapas extras
- teclado automatico, foco no valor, moeda em real e composicao visual validados no aparelho
- tres providers de widget e atalho dinamico de captura confirmados pelo sistema
- build e lint devem permanecer sem erros a cada entrega
- repetir a bateria visual e funcional em aparelho real quando o ADB estiver conectado
- confirmar widget, notificacao e atualizacao do icone em mais de um launcher
- teste instrumentado da captura rapida executado com sucesso no Xiaomi
- testes instrumentados de autenticacao e fila offline executados com sucesso no Xiaomi
- 19 testes instrumentados executados com sucesso no Xiaomi Android 15
- testes JVM, `assembleDebug`, `assembleDebugAndroidTest` e `lintDebug` concluidos sem falhas
- tema claro restaurado depois da verificacao isolada do tema escuro
- Fases 5 e 6 cobertas por 25 testes instrumentados no Xiaomi Android 15
- deep link de convite e telas Compartilhado/Ajustes revisados visualmente no aparelho
- sincronizacao real e contratos online das Fases 1, 5 e 6 validados com conta administrativa
