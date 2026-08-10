# Roadmap Android - Finanza Next

### Atualizacao tecnica - widgets e dashboard Web - 04/08/2026

- Os widgets nativos agora respeitam o modo claro em todas as experiencias: Web preserva o fundo verde do Finanza, enquanto Moderno e Classico usam superficie clara e o acento configurado pelo usuario. Isso corrige o widget escuro aparecendo em temas claros sem alterar o visual do app Classico.
- O historico da experiencia Finanza Web agora oferece faixa de valor minimo/maximo e ordenacao por data ou valor, mantendo busca, tipo, categoria e conta; esses controles nao aparecem no Moderno ou no Classico.
- O estado composto da tela principal deixou de ser reconstruido a cada recomposicao: `MainActivity` agora mantem um `AppUiState` observavel e o atualiza apenas em `render()`, reduzindo trabalho durante troca por toque ou gesto sem mudar a navegacao nem o visual do Classico.
- A bateria local passou em `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, `:app:lintDebug` e `:app:assembleDebug`.
- O APK validado localmente nesta rodada esta em `android-kotlin/app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `27258640D067956C60CCEEF4A777E87AFBBB33796792AC282D4C607A5C4E5A78`.
- O mesmo APK foi enviado por `adb push` para `/sdcard/Download/Finext-debug.apk`; a instalacao automatica ainda retorna `INSTALL_FAILED_USER_RESTRICTED`, portanto a abertura manual no aparelho continua sendo o proximo gate de validacao.
- A sincronizacao agora aplica uma lista remota de contas vazia como estado valido, limpando contas antigas locais; quando o campo nao existe na resposta, o cache e preservado para compatibilidade.
- APK apos essa correcao: SHA-256 `5930A61461C7F79A29972399CD23E34F631FE3F122646EAFE9628E46DE5B49CC`, reenviado ao mesmo caminho no aparelho.
- A experiencia Finanza Web agora exibe uma central de cartoes com fechamento, vencimento, validade e final do cartao a partir dos dados sincronizados; Moderno e Classico mantem o comportamento visual anterior.
- A regressao local de contas Web foi compilada junto com a bateria de testes; APK desta rodada: SHA-256 `02A4BF6C221E1FE9529F8B4ED82BAA5B7EE18751323F3A44D95F301DDCDD615F`.
- O resumo de vencimentos do tema Web agora mostra compromissos, vencidos e proximo vencimento antes da lista, mantendo o Classico e o Moderno sem essa alteracao.
- APK desta rodada apos a melhoria de vencimentos: SHA-256 `D620D9747F72CCA669478B49847DEB28D3386D2E01EACB16691F7FD20CA3AF84`, reenviado para `/sdcard/Download/Finext-debug.apk`.
- O historico Web agora tambem oferece os periodos 7 dias, 1 mes, 3 meses, 6 meses e tudo, combinaveis com valor, ordenacao e filtros existentes; Moderno e Classico nao exibem esse controle extra.
- APK apos essa etapa: SHA-256 `94529D4DDC2B92676394F40FDA70CED2340D4ABACA5F54526B4809D4672FE0D2`, reenviado para `/sdcard/Download/Finext-debug.apk`.
- O resumo de vencimentos do tema Web agora mostra compromissos, vencidos e proximo vencimento antes da lista, mantendo o Classico e o Moderno sem essa alteracao.
- APK desta rodada apos a melhoria de vencimentos: SHA-256 `D620D9747F72CCA669478B49847DEB28D3386D2E01EACB16691F7FD20CA3AF84`, reenviado para `/sdcard/Download/Finext-debug.apk`.
- `git diff --check` passou. A instalacao e os testes instrumentados continuam bloqueados pelo HyperOS com `INSTALL_FAILED_USER_RESTRICTED`.
- Nova tentativa com o APK final em 04/08/2026 confirmou a mesma restricao no aparelho conectado; nenhum teste visual autenticado foi declarado como aprovado.
- A regressao `ThemeTokenIsolationTest` agora verifica os tokens efetivos do Moderno escuro e do Finanza Web claro/escuro, incluindo fundo e acento; o Classico nao foi alterado. A compilacao instrumentada passou novamente.
- O calendario agora tem regressao dedicada para a grade mensal Web/Moderno: evento clicavel, resumo de vencimentos e renderizacao do mes atual. O APK foi recompilado com a identificacao semantica dos eventos e reenviado para `/sdcard/Download/Finext-debug.apk`; SHA-256 `9889D1DFFEC6AF6EB2769B15A0F6A2BA05BA950C6A1ABE497579854F5BAD8625`.

## Roadmap vigente - 22/07/2026

### Atualizacao - 04/08/2026

- A primeira tela do Finanza Web foi alinhada ao shell real de `index.html`: o Android agora usa o titulo `Início`, resumo de saldo destacado, entradas e saídas em dois blocos compactos e sobra projetada/contas a pagar na segunda linha. A alteração ficou isolada em `FinanzaWebHome.kt`; Moderno e Clássico não foram tocados.
- Depois desse ajuste passaram novamente `:app:testDebugUnitTest`, `:app:lintDebug` e `:app:assembleDebug`; a APK atual tem SHA-256 `4AE7CA0EE6B502AC1BAB244E50512183548BF89A5CE5D460CDD2A0668DC39464`.
- Foi adicionada a regressão `FinanzaWebHomeExperienceTest`, cobrindo os textos e a hierarquia mínima do shell Web; `:app:compileDebugAndroidTestKotlin` e `:app:testDebugUnitTest` passaram.

- A conferencia direta de `Nubank_2026-07-23.pdf` confirmou o formato usado nos testes: as compras ficam nas paginas 5 e 6, enquanto resumo da fatura, pagamento anterior, juros, limites, saldo em aberto e pagamentos ficam nas paginas 1-4 e 7. O parser permanece restrito a linhas de compra dentro da secao `TRANSAÇÕES DE` e nao transforma esses valores administrativos em gastos.
- O fluxo de fatura tambem cobre os casos reais da pagina 6: `Amazon BR III - NuPay` com estorno integral e recobranca posterior continua como uma unica compra efetiva; `Shopee *Digitalcalotas` com estorno parcial e `Korfit Academia - Parcela 6/12` entram na revisao com reconciliacao e busca de parcela.
- A bateria JVM, lint e empacotamento foram repetidos apos limpeza: `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebugAndroidTest` e `:app:assembleDebug` passaram. A APK atual esta em `android-kotlin/app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `EBC9DBE35B36E9F111F90E1DD36959B7EF4710F5017CDB00EAEFFC96BC3755D1`.
- A validacao conectada chegou ao aparelho, mas o HyperOS continua recusando a instalacao automatica do APK de testes com `INSTALL_FAILED_USER_RESTRICTED`. A instalacao manual que havia funcionado anteriormente nao pode ser repetida nesta rodada porque a permissao de instalacao via USB voltou a ser negada; a bateria instrumentada e a validacao visual autenticada seguem pendentes dessa permissao do aparelho.

- O snapshot Android passou a percorrer transacoes por `offset` quando a API retorna `total` sem cursor. A pagina seguinte e deduplicada por `id`; se o backend passar a expor `next_cursor|nextCursor`, o cursor permanece o caminho prioritario. A cobertura instrumentada do repositorio inclui os dois protocolos.
- A folha de Aparencia agora mostra amostras independentes de canvas, superficie e acento para Moderno, Classico e Finanza Web nos modos claro e escuro. A escolha continua pendente ate `Aplicar aparencia`, portanto a tela nao altera preferencias enquanto e explorada.
- Validacao parcial no Xiaomi Android 15: APK `1.3.0-finext` instalada em 04/08/2026, tela de login renderizada sem crash e providers de captura rapida, resumo e agenda registrados no sistema. O Xiaomi bloqueia digitacao por ADB, portanto a validacao autenticada de temas, navegacao, widgets fixados e desempenho depende do login manual no aparelho. A leitura de abertura no login (p50 14 ms, p90 30 ms) nao e usada como meta do fluxo financeiro autenticado.

- A importacao de fatura Nubank passou a ler somente as linhas da secao explicita de transacoes. Limites, juros, pagamentos, simulacoes e totais nao entram como lancamentos.
- Estornos sao reconciliados com a compra correspondente antes da revisao; a cobranca efetiva permanece disponivel para confirmacao.
- A revisao de fatura agora compara comerciante, valor, data e marcador de parcela. Nomes abreviados como `Korfit 4/12` podem localizar um lancamento existente como `Korfit Academia`, mas parcelas provaveis continuam desmarcadas para decisao manual.
- O calendario foi ampliado para o mes inteiro, com navegacao por mes, eventos reais e barra inferior reservada fora da grade. Registros administrativos antigos da fatura sao ocultados apenas da agenda; os dados locais nao sao apagados.
- O APK debug atual foi recompilado e os testes JVM de dominio e revisao de fatura passaram. A instalacao no Xiaomi continua bloqueada por `INSTALL_FAILED_USER_RESTRICTED`, portanto a captura visual desta revisao ainda depende da autorizacao de instalacao por USB.
- Conferencia adicional da `Nubank_2026-07-23.pdf`: linhas administrativas que aparecem depois da primeira compra dentro da mesma secao tambem sao descartadas (limite, total a pagar, pagamento e saldo restante), sem depender da ordem das paginas do PDF.
- O APK desta rodada foi gerado em `android-kotlin/app/build/outputs/apk/debug/app-debug.apk` e copiado para `/sdcard/Download/Finext-debug.apk`; a validacao visual, dos widgets e de desempenho no aparelho segue pendente enquanto o HyperOS nao autorizar a instalacao.
- O dashboard do tema Finanza Web agora calcula os cards de fluxo com movimentos do mes ainda em aberto, excluindo lancamentos quitados como no `widgetCards()` do web; esses lancamentos continuam no historico. O Moderno e o Classico nao receberam essa regra visual.
- A renda base mensal agora pode ser configurada em Ajustes no Finanza Web, persiste localmente, entra no dashboard e acompanha backup/sincronizacao; o Moderno e o Classico nao receberam esse campo visual.
- A validacao desta rodada passou em `:app:testDebugUnitTest`, `:app:assembleDebugAndroidTest` e `:app:assembleDebug`; o APK foi reenviado para `/sdcard/Download/Finext-debug.apk`.
- O parser agora reconcilia estornos dentro da secao de compras: estorno parcial reduz a compra ao valor liquido, estorno integral remove a cobranca anterior e uma recobranca posterior permanece uma unica despesa. O comportamento foi coberto com os casos reais de Shopee e Amazon da fatura Nubank.
- Quando a compra bruta ja existe no historico, a revisao identifica o ajuste por estorno, deixa a linha para confirmacao manual e atualiza o mesmo lancamento pelo ID; isso evita importar o valor liquido como uma segunda despesa.
- Os widgets nativos agora seguem o fluxo aberto do dashboard Web: ignoram movimentos quitados e futuros e usam a renda mensal configurada quando existir, sem alterar o layout visual por experiencia.
- A bateria desta rodada passou em `:app:testDebugUnitTest`, `:app:assembleDebugAndroidTest` e `:app:assembleDebug`; APK reenviado ao aparelho com SHA-256 `A3D099582A9F76A439BE7B9F8CCFD2964C822689FB84D558345FBE08DD04EFEE`.
- A verificacao local adicional passou em `:app:lintDebug` e confirmou os quatro componentes de sistema no manifesto (captura rapida, resumo, agenda e entrada inline). O teste conectado foi tentado no Xiaomi, mas continuou sem executar testes porque o HyperOS bloqueou a instalacao com `INSTALL_FAILED_USER_RESTRICTED`.

### Principio de produto

- O **Finanza web** e a fonte de regras, dados, contratos e escopo funcional.
- O **Moderno** usa a linguagem fluida do Next/The Box.
- O **Classico** e uma experiencia preservada: nao recebe reformulacoes visuais fora de correcoes.
- O **Finanza Web** reproduz a linguagem visual mobile do site, sem criar um segundo aplicativo ou uma segunda base de dados.
- As tres experiencias usam a mesma sessao, sincronizacao, navegacao, dados e modulos. Tema nunca altera uma regra financeira.

### Estado honesto hoje

- O nucleo funcional das fases 1 a 6 esta implementado: sessao, fila offline, transacoes, contas, cartoes, vencimentos, planejamento, importacao, carro, compartilhado e administracao.
- Historicos acima de 1.000 transacoes agora sao carregados por paginas com `offset`, usando o `total` retornado pela API. Um cursor de backend continua desejavel para estabilidade durante sincronizacoes muito concorrentes, mas nao bloqueia mais a leitura completa.
- A paridade visual tambem continua em andamento. O Classico esta congelado, enquanto Moderno e Finanza Web precisam de revisao por tela no aparelho real.

### Atualizacao - 02/08/2026

- A selecao de Aparencia deixou de usar o dialogo legado de Views e agora abre uma folha Compose responsiva. A escolha de modo e tema so persiste ao confirmar, e a folha foi revisada no Xiaomi em Moderno escuro.
- O Moderno escuro teve o vidro da navbar e o estado selecionado suavizados sem alterar a geometria The Box. O Inicio tambem deixou de repetir Contas, Analise e Ajustes: agora apresenta somente uma entrada compacta para a Central de recursos, seguida pelo saldo e vencimentos.
- A revisao visual do Finanza Web reduziu as bordas de alta opacidade no Inicio, Contas, Analises e Ajustes. O Classico nao foi alterado.
- A importacao de fatura em PDF ganhou uma revisao dedicada no Android: cada linha e pesquisavel e selecionavel antes da confirmacao; duplicatas exatas sao bloqueadas e parcelas provaveis ficam desmarcadas ate confirmacao explicita. PDFs escaneados usam OCR sob demanda pelo Google Play Services; a primeira leitura requer conexao para disponibilizar o modulo.
- A Agenda agora abre diretamente pelo modulo, sem cair no formulario generico da Central. Ela usa uma janela de tres semanas, com lancamentos e vencimentos reais em colunas por dia, aviso resumido e barra fixa para trocar semana ou abrir a captura rapida. A captura foi validada no Xiaomi: o botao central abre o fluxo de valor com `R$ 0,00` e teclado numerico.
- A medicao aquecida mais recente do fluxo principal no Xiaomi Android 15 registrou 1.101 quadros, 0,82% de quadros lentos e percentil 90 de 8 ms. Essa leitura nao substitui uma bateria comparativa de todas as telas e aparelhos.
- A entrega foi compilada com testes JVM, lint e APK debug. A validação deste APK no aparelho continua pendente: o HyperOS recusou a instalação por USB (`INSTALL_FAILED_USER_RESTRICTED`).
- A revisão de fatura também reconhece o marcador compacto de extrato `03/10`. Ele só sugere uma parcela quando encontra um lançamento já estruturado como parcelado, preservando a revisão manual e evitando tratar datas como cobranças.
- O parser de PDF/texto recompõe lançamentos quebrados em linhas separadas de data, descrição e valor, e ignora totais de fatura. O Dashboard do tema Finanza Web agora abre diretamente a Central de importação, que concentra PDF, CSV/OFX, JSON, texto/OCR, histórico e sincronização.
- O Histórico agora é uma tela Compose própria, aberta pelo painel e pelos widgets de transações: busca descrição ou categoria, combina filtros de entradas/gastos, categoria e conta, agrupa por mês e abre o lançamento original para edição.
- As telas financeiras passaram a carregar a data ISO separadamente da data exibida. Isso corrige ordenação, filtros de período e os cálculos dos widgets semanal e mensal sem alterar a apresentação do tema Clássico.
- A auditoria visual no Xiaomi confirmou o Finanza Web escuro com hierarquia própria (acento lime, resumo de saldo, blocos operacionais e navegação reta). O Moderno manteve navbar The Box e reduziu mais uma borda no comando de editar o painel.
- Em navegação aquecida por abas no Xiaomi Android 15, a medição mais recente registrou 602 quadros, 1,00% de quadros lentos, p50 de 5 ms e p90 de 6 ms. Há picos isolados de inicialização a investigar; a troca contínua não apresenta lentidão sustentada.

### Fase A - Fechamento visual e de interacao

1. Criar uma matriz de telas para Moderno e Finanza Web: inicio, contas, analise, historico, configuracoes, central, login, modais e captura rapida, em claro e escuro.
2. Comparar cada tela autenticada com capturas do Finanza web mobile e registrar diferencas de hierarquia, espacamento, cores, estados vazios, formularios e acoes.
3. No Moderno, alinhar Inicio e Analise ao nivel de acabamento de Contas; reduzir bordas e superficies concorrentes, especialmente no escuro.
4. No Finanza Web, transferir os tokens e a estrutura do web por componente, sem alterar o Classico nem reutilizar o visual do Moderno por engano.
5. Executar testes de gesto, navbar, modal de tema, teclado, insets e acessibilidade em tela pequena e grande.

**Criterio de saida:** capturas aprovadas de todas as telas essenciais nos dois temas alvo, sem regressao do Classico.

### Fase B - Confiabilidade e desempenho

**Medicao inicial no Xiaomi Android 15 (24/07/2026):** abertura fria da `MainActivity` em 1,186 s e abertura fria da captura rapida em 0,988 s. A captura foi conferida por hierarquia de acessibilidade: janela no topo, campo `R$ 0,00` com foco e teclado solicitado. A recompilacao mais recente manteve o comportamento: `MainActivity` em 1,417 s ate a tela de login e atalho de captura em 1,035 s. Uma repeticao posterior ficou em 1,425-1,458 s para a abertura fria da tela de login; a variacao ainda precisa ser medida com sessao autenticada, dados reais e mais de um fabricante.

1. Medir abertura fria, troca por toque, gesto lateral, rolagem e captura rapida em aparelhos reais.
2. Manter sincronizacao e calculos financeiros fora do caminho de troca de abas; investigar recomposicoes antes de adicionar animacoes.
3. Instrumentar erros de sincronizacao, tentativas da fila offline, duracao de importacoes e falhas de widgets, sem gravar dados financeiros sensiveis em logs.
4. Validar reconexao, sessao expirada, conflito de escrita e dados parcialmente sincronizados.

**Criterio de saida:** metas de tempo documentadas por aparelho e fluxos offline recuperaveis, com mensagem clara para a pessoa usuaria.

### Fase C - Paridade funcional mensuravel

1. Ampliar o backend com paginacao por cursor para transacoes, contas e registros de importacao. O Android ja junta as paginas de transacoes por `offset` e deduplica por `id`; quando `next_cursor|nextCursor` chegar, ele sera priorizado para leituras concorrentes mais estaveis.
2. Consolidar categorias personalizadas em novos seletores que vierem a ser criados; captura rapida e sugestoes do lancamento completo ja usam as categorias sincronizadas.
3. Evoluir o Histórico Android: a busca, os filtros compostos por tipo/categoria/conta, o agrupamento mensal e os atalhos estão entregues; faltam os estados e atalhos avançados comparáveis ao web.
4. Reproduzir a central de importacao: historico, progresso, erros, revisao de conflitos e reprocessamento.
5. Comparar modulos um a um com a conta web: orcamentos, metas, assinaturas, dividas, contratos, compras, veiculos e compartilhado.

**Criterio de saida:** cada modulo publicado possui leitura, criacao, edicao, exclusao, persistencia, sincronizacao, offline e estados de erro comprovados.

**Auditoria de 24/07/2026:** Categorias, orcamentos, metas, assinaturas, dividas, contratos, listas, compras, espaco compartilhado, pessoas, veiculos e carro passam pelo mesmo `FinanzaFeatureStore`. As acoes de criar, editar, excluir e acao primaria gravam primeiro no aparelho e seguem pela fila offline quando a sincronizacao falha. Metas e orcamentos usam as rotas especificas da API; os demais modulos atualizam o estado agregado documentado pelo web. Nao foram encontrados modulos publicados apenas como interface nesta revisao.

### Fase D - Captura e widgets como produto de sistema

**Verificacao de pacote no Xiaomi (24/07/2026):** os providers de captura rapida, resumo e agenda estao registrados no APK `1.3.0-finext`; todos declaram layout inicial, redimensionamento e categoria de tela inicial. A aparencia em launchers de outros fabricantes ainda precisa de validacao manual.

**Revalidacao no Xiaomi (24/07/2026):** os tres providers continuam registrados depois da atualizacao do APK. O atalho abriu `QuickExpenseActivity` em janela compacta `[0,122]-[1080,600]`; o painel interno ocupa `[83,161]-[997,561]` e o campo `R$ 0,00` recebeu foco automaticamente. A validacao visual de um widget fixado na tela inicial continua pendente, pois depende da adicao manual no launcher.

**Paridade do painel (24/07/2026):** o editor de widgets do dashboard agora inclui renovacoes, lista de compras, veiculos, mini estatisticas, taxa de economia, ritmos semanal/mensal/anual e graficos. Os ritmos reproduzem as leituras do web com totais e medias por periodo, mais a comparacao da semana anterior. Todos entram desativados por padrao, respeitam a configuracao individual de cada experiencia e usam o estado financeiro ja carregado; portanto nao alteram o painel atual nem acrescentam requisicoes de rede na troca de abas.

1. Validar widgets em Pixel, Samsung, Xiaomi e launchers alternativos: tamanho, transparencias, atualizacao, toque e contraste claro/escuro.
2. Validar atalho do launcher, notificacao inline e captura rapida contra bloqueio de tela, retorno ao app e falta de rede.
3. Criar um conjunto pequeno de widgets configuraveis por usuario, mantendo o widget de captura como o caminho mais rapido para valor e descricao.

**Criterio de saida:** captura de gasto funciona em poucos segundos, sem abrir a tela principal, e o widget continua legivel nos launchers suportados.

### Fase E - Qualidade de lancamento

1. Build release assinado, reducao de tamanho, R8, politica de privacidade, textos legais e checklist Play Store.
2. Testes em uma matriz de Android 8+ e fabricantes, com migracao de dados entre versoes.
3. Acessibilidade: leitores de tela, escala de fonte, contraste, navegacao por teclado e idioma.
4. Backup e restauracao de emergencia, diagnostico exportavel e suporte a recuperacao de conta.

**Criterio de saida:** release candidata instalavel, observavel e recuperavel, com plano de rollback.

### Depois do 1.0

- Regras de automacao pessoal (categorizar, alertar, sugerir e conciliar), sempre opt-in e explicaveis.
- Notificacoes inteligentes de vencimento, orçamento e anomalia, com controle fino por conta e categoria.
- Mais analises comparativas e previsao de fluxo de caixa, sem substituir os dados reais por estimativas opacas.
- Compartilhado com aprovacao configuravel, liquidacao guiada e trilha de auditoria melhorada.
- Integracoes financeiras apenas quando houver consentimento, seguranca, suporte operacional e contratos de dados claros.

Objetivo atual: levar as funcionalidades do Finanza web para um app Android basico e rapido, usando a linguagem visual e de interacao do The Box no iOS.

## Referencias do produto

- Finanza web: fonte de funcionalidades, regras, dados e sincronizacao
- The Box no iOS: fonte do visual, navegacao, movimentos e captura rapida
- o Android nao copia a interface web e nao inventa regras ausentes no Finanza

## Atualizacao de arquitetura e entrega

O Android passa a ser **um unico app funcional com tres experiencias completas**. Moderno, Classico e Finanza Web nao sao tratados como aplicativos, bancos de dados ou navegacoes paralelas. Todo recurso entra uma vez e deve funcionar nas tres experiencias.

- **Dados, regras e fluxos:** Finanza web.
- **Tema Moderno:** linguagem atual, compacta e de alto contraste.
- **Tema Classico:** experiencia preservada, sem reformulacao visual fora de correcoes.
- **Tema Finanza Web:** aplicacao mobile direta da gramatica do Finanza web: fundo grafite em camadas, superficies translucidas, bordas discretas, lime como acao primaria, teal e roxo como sinais secundarios.

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

### Atualização em 22/07/2026

- O app passou a ter uma introdução de primeira abertura, curta e opcional, com três etapas: painel, Central de módulos e captura rápida. Ela aparece depois do login ou da escolha pelo modo local e não volta a aparecer após a conclusão.
- O tema **Moderno** recebeu nova harmonização sem alterar o **Clássico**: a Análise agora concentra entradas e saídas em uma superfície única, no mesmo vocabulário da tela Contas; o mosaico do Início não usa mais borda clara; e a troca de aba por toque usa troca imediata, preservando o gesto lateral.
- O tema **Finanza Web** passou a reproduzir o cabeçalho mobile da página inicial do site: `Início`, texto operacional, período, personalização e ação `+ Nova`. A paleta e a navegação inferior seguem os tokens do `styles.css` do projeto web.
- `lintDebug`, testes JVM e `assembleDebug` passaram nesta rodada. O novo teste instrumentado do onboarding compilou, mas a execução no Xiaomi foi bloqueada antes de iniciar porque o HyperOS recusou a instalação do APK de testes (`INSTALL_FAILED_USER_RESTRICTED`).
- A próxima validação em aparelho continua pendente de liberar novamente a instalação por USB. Ela deve cobrir onboarding, Moderno escuro, Finanza Web claro/escuro, transição entre abas, modal de tema e captura rápida.
- A janela de Aparência passou a manter a prévia coerente enquanto o usuário alterna Claro/Escuro: as amostras dos temas seguem a escolha ainda não aplicada, e a preferência só é salva ao confirmar a ação.
- Os filtros de valor em Análise (Moderno e Finanza Web) agora ignoram texto incompleto ou inválido até que um valor monetário válido seja informado; isso evita que um rascunho seja tratado como `R$ 0,00`.
- O APK debug foi recompilado e reinstalado com sucesso no Xiaomi. A tela de entrada foi revisada por captura nativa; as telas autenticadas continuam pendentes porque o aparelho não aceita toques ADB e estava em uso fora do Finext durante a última verificação.
- A navbar do tema **Moderno escuro** passou a usar uma superfície mais opaca, sem borda e sem sombra escura; o item ativo ganhou contraste próprio. Os componentes do **Clássico** e do **Finanza Web** não foram alterados nessa revisão.
- O contraste das barras de sistema no login claro foi corrigido com `WindowCompat`, inclusive após a janela recuperar foco no HyperOS. O aparelho foi desconectado antes da nova validação das telas autenticadas.
- O scaffold do tema **Finanza Web** deixou de desenhar halos decorativos sobre o conteúdo. A tela usa apenas o canvas e as superfícies da paleta do web, como a referência mobile.

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
- Contas no tema Finanza inicia diretamente na grade de carteiras do web; o resumo consolidado redundante foi mantido apenas no Next, sem remover transferencia ou a agenda de vencimentos
- a troca de aba por toque agora usa a mesma confirmacao unica do gesto lateral: o indicador responde de imediato, o pager anima a troca e os dados sao atualizados somente apos a pagina estabilizar
- revisao visual por captura nativa no Xiaomi confirmou Dashboard e Contas Finanza escuros: titulo sem quebra, grade de contas legivel e barra The Box flutuante sem uma faixa de fundo adicional
- Analise, Ajustes e captura rapida tambem foram revisados por captura nativa no Xiaomi: os tokens escuros permanecem consistentes, e o atalho abre um painel compacto com teclado automatico sem iniciar a tela principal
- a bateria instrumentada no Xiaomi agora possui 40 testes, incluindo guardrails para a ausencia de ActionBar nativa, a ordem da barra inferior e a grade de resumo Finanza sem a faixa redundante de widgets
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

### Estado verificado nesta entrega

- tema Clássico permaneceu fora das alterações visuais; os ajustes recentes foram limitados ao Moderno e ao tema Web
- o Dashboard do tema Web deixou de usar o hero genérico e agora reproduz as decisões do dashboard mobile do Finanza: renda, gasto mensal, seguro diário, sobra projetada e contas a pagar
- a composição principal deixou de reler preferências por lançamento e de decodificar `entries` repetidamente ao calcular o mês e os saldos das contas
- calendário Moderno recebeu saldo diário e sinais explícitos de entrada/saída nos cartões de evento, preservando o calendário dos demais temas
- revisão de fatura já aceita PDF textual, mostra prévia e bloqueia duplicatas exatas; parcelas reconhecidas são sinalizadas antes da confirmação
- PDFs escaneados agora usam OCR sob demanda pelo Google Play Services, limitado à importação e sem incorporar o modelo de visão na APK; a primeira leitura requer conexão para disponibilizar o módulo
- build debug, testes JVM, compilação dos testes Android e lint passaram nesta entrega; a APK debug final mede 30,13 MB
- a APK atual foi instalada via ADB no Xiaomi (`f0540ca9`); os três providers de widget foram encontrados no sistema
- a sessão do Finext no aparelho está atualmente na tela de login. Por isso, sincronização real, desempenho das listas com dados da conta e atualização visual dos widgets ainda precisam de validação autenticada nesta instalação
- o único perfil de renderização coletado após abertura exibiu cinco quadros e não é representativo da navegação; não deve ser usado como conclusão de desempenho

### Validação no Xiaomi - 03/08/2026

- APK `1.3.0-finext` reconstruída com testes JVM e instalada por atualização, preservando os dados do aparelho
- sessão autenticada confirmada, com 26 widgets de painel e 90 lançamentos carregados na visão geral
- navegação real entre Início, Contas e Análise não apresentou crash; após aquecimento, a amostra de 85 frames teve 1,18% de jank, mediana de 8 ms e p95 de 17 ms
- os providers de widget rápido, resumo e agenda continuam registrados no Android
- Agenda foi aberta sem alterar dados; o calendário concentra gastos, vencimentos e entrada rápida, e deve receber validação visual novamente quando houver eventos dentro da janela exibida
- revisão de fatura permanece pendente de homologação com um PDF de fatura real ou anonimizado; a importação não deve ser confirmada automaticamente
- quando a revisão encontra uma parcela futura já planejada, ela agora oferece remoção individual com confirmação; a linha importada continua exigindo seleção manual
- revisão no Xiaomi confirmou o Finanza Web claro: o cabeçalho do Dashboard mantém o título inteiro em telas estreitas e as contas usam um único cartão, sem uma superfície interna duplicada
- o tema do aparelho foi restaurado para Moderno claro após a validação; o tema Clássico não foi aberto nem alterado
- quando a máquina trava na detecção de sistemas de arquivos do Gradle, usar `--no-watch-fs` permite compilar APK, testes JVM e testes Android sem encerrar o Android Studio

### Estado de validação posterior - 03/08/2026

- o calendário agora normaliza valores assinados antes de renderizar: o sinal é exibido uma única vez (`-R$ 16,00`), evitando cartões como `-R$ -16,00`
- a normalização e a formatação monetária do calendário agora têm teste JVM dedicado, cobrindo sinais, separador de milhares e centavos em pt-BR
- a grade do calendário Moderno foi compactada para exibir mais semanas e eventos na mesma tela; as alturas do Clássico e do Finanza Web ficaram inalteradas
- o calendário passou a formatar totais e valores de evento em pt-BR com duas casas e separador de milhares; a correção evita valores como `R$ 60270` na agenda
- auditoria de isolamento restaurou os tokens e a navbar originais do tema Clássico; a paleta Web permanece em tokens próprios e a nova barra Web continua limitada ao branch `AppExperience.WEB`

- teste adicional baseado na fatura real `Nubank_2026-07-23.pdf`: cinco compras válidas foram mantidas; resumo, limite, juros, pagamentos e saldo restante foram excluídos
- `Korfit 6/12` contra uma parcela planejada `Korfit 4/12` agora é coberto explicitamente e permanece como sugestão desmarcada para confirmação manual

- a bateria JVM passou após os ajustes de reconciliação de fatura e da janela de captura rápida; `assembleDebug` também concluiu sem falhas com `--no-watch-fs`
- o teste instrumentado da revisão de fatura compilou, mas o HyperOS cancelou a instalação do APK de testes com `INSTALL_FAILED_USER_RESTRICTED` antes de executar qualquer teste
- a tentativa de reinstalar o APK normal via ADB recebeu a mesma restrição. O APK debug atualizado foi copiado para `Downloads/Finext-debug.apk` no Xiaomi para instalação manual pelo gerenciador de arquivos
- até essa instalação manual ser confirmada, a sessão autenticada, a Agenda com dados reais, os widgets no launcher e as métricas de desempenho não devem ser considerados revalidados nesta rodada
- a captura rápida passou a declarar `edgeToEdge = false` somente na atividade flutuante. Isso preserva a composição edge-to-edge do app principal e impede que a folha fique sob a barra de status
- a revisão de fatura sugere uma parcela planejada quando comerciante, valor e total da série coincidem mesmo com numeração diferente; a sugestão permanece desmarcada e exige confirmação, enquanto uma compra sem marcador de parcela continua como lançamento novo
- a fatura Nubank `Nubank_2026-07-23.pdf` foi conferida: o parser agora lê linhas no formato `16 JUN ... Korfit ... R$ 149,00`, remove data e final do cartão da descrição, ignora cabeçalhos/limites/juros/pagamentos e reconcilia estornos sem importar a cobrança administrativa
- a revisão mantém a confirmação manual e a busca de parcelas; `Korfit Academia - Parcela 6/12` fica pronta para comparar com lançamentos já planejados antes de importar
- regressão JVM passou com 10 testes de regras de domínio e 7 de revisão de fatura; APK debug final gerada em `android-kotlin/app/build/outputs/apk/debug/app-debug.apk` (30.788.386 bytes após build limpo)
- a navbar do tema Finanza Web deixou de desenhar uma borda nos quatro lados; agora mantém somente a linha superior discreta do web original, sem alterar as barras Moderno ou Clássico
- o pager principal passou a pré-compor somente a aba adjacente, em vez de manter duas abas extras de cada lado; isso reduz trabalho de composição na troca por toque e pelo gesto lateral sem remover o swipe
