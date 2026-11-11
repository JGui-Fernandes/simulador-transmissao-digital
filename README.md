# Checklist do Projeto: Simulador de Transmissão Digital

Marque cada item conforme for concluído. Cada etapa só começa quando a anterior estiver com todos os itens marcados.

## Etapa 1: Núcleo (tipos básicos)
**Objetivo:** criar os tipos que todas as outras etapas usam.

- [x] `Complex` (record com `re` e `im`): soma, multiplicação, módulo quadrado e distância
- [x] `Modulation` (enum BPSK, QPSK, QAM16): `k`, constelação com Gray e eficiência espectral
- [x] `DataGenerator.textoParaBits(String)` (ASCII, 8 bits por caractere, MSB primeiro)
- [x] `DataGenerator.bitsParaTexto(int[])`
- [x] `DataGenerator.bitsAleatorios(n, semente)`
- [x] Teste: ida e volta do texto (ASCII puro)
- [x] Teste: mesma semente gera a mesma sequência
- [x] Teste: energia média da constelação igual a 1 (pode ser adiado para a Etapa 6)

**Pronto quando:** testes de `DataGenerator` e `Complex` passam.

---

## Etapa 2: Codificação de linha (banda base)
**Objetivo:** implementar AMI e Manchester sem canal, com ida e volta exata.

- [x] `LineCode` (enum AMI, MANCHESTER)
- [x] `LineEncoder`: AMI (0 vira 0; 1 alterna +1 e −1)
- [x] `LineEncoder`: Manchester (2 amostras por bit, convenção documentada)
- [x] `LineDecoder`: AMI por magnitude (|x| > 0,5)
- [x] `LineDecoder`: Manchester pela diferença entre as metades (r1 − r2)
- [x] Teste: ida e volta sem ruído para AMI e Manchester
- [x] Teste: AMI nunca repete polaridade entre pulsos consecutivos
- [x] Teste: Manchester tem transição no meio de cada bit

**Pronto quando:** a mensagem de texto é recuperada exatamente nos dois códigos, sem ruído.

---

## Etapa 3: Canal AWGN, conversão de SNR e BER
**Objetivo:** adicionar ruído com SNR controlado e medir a BER.

- [x] `Channel` (interface): `Complex[] apply(Complex[] sinal, double sigma, Random rnd)`
- [x] `AwgnChannel`: ruído gaussiano com variância σ² por componente
- [x] `SnrConverter`: Eb/N0 → Es/N0 (com k e R)
- [x] `SnrConverter`: Es/N0 → σ
- [x] `SnrConverter`: σ para banda base, com amostras por bit (1 para AMI, 2 para Manchester)
- [x] `Metrics.ber(enviados, recebidos)`: BER e índices dos bits errados
- [x] Teste: variância medida do ruído próxima da teórica
- [x] Teste: `SnrConverter` nos dois sentidos (0 dB, k = 2, R = 1 → 3,01 dB)
- [x] Teste: BER de AMI e Manchester sem ruído é zero
- [x] Teste contra a teoria: NRZ polar, 10⁶ bits, 4 dB → BER ≈ 1,25·10⁻²

**Pronto quando:** o teste contra a teoria do NRZ polar passa.

---

## Etapa 4: Codificação de canal (Hamming 7,4)
**Objetivo:** implementar a correção de erros.

- [x] `ChannelEncoder` (interface) e `ChannelDecoder` (interface)
- [x] `Hamming74`: codificação de blocos de 4 bits em 7, com padding registrado
- [x] `Hamming74`: decodificação por síndrome (corrige 1 erro por bloco)
- [x] Teste: os 16 blocos de dados são recuperados sem ruído
- [x] Teste: todo erro simples (7 posições) é corrigido
- [x] Teste: erro duplo no mesmo bloco não é corrigido (documentando o limite)
- [x] Teste: taxa R = 4/7 usada corretamente no `SnrConverter`

**Pronto quando:** pipeline banda base com Hamming passa sem ruído e, em Eb/N0 alto, a BER fica abaixo da BER sem código.

---

## Etapa 5: Canal de rajadas (Gilbert-Elliott)
**Objetivo:** implementar o segundo cenário de canal, com erros agrupados.

- [ ] `BurstChannel` (implementa `Channel`): cadeia de Markov com dois estados
- [ ] Estado bom (σ normal) e estado ruim (σ × fator de degradação)
- [ ] Parâmetros p (entrar no ruim) e r (sair do ruim) configuráveis
- [ ] Estado mantido entre chamadas, dependente só do `Random` recebido
- [ ] Teste: fração de amostras no estado ruim perto de p / (p + r)
- [ ] Teste: mesma semente gera a mesma sequência de estados
- [ ] Teste: erros agrupados (dois erros consecutivos mais prováveis que em canal independente)
- [ ] Calibração com cenário piloto (p, r e fator escolhidos e documentados)

**Pronto quando:** o cenário piloto mostra BER maior que o AWGN no mesmo Eb/N0 médio e erros agrupados.

---

## Etapa 6: Modulação e demodulação
**Objetivo:** implementar BPSK, QPSK e 16-QAM com Gray.

- [ ] `Modulator`: converte bits em `Complex[]`, agrupando k bits por símbolo
- [ ] `Demodulator`: decisão por distância mínima e devolve bits
- [ ] Padding documentado para bits que não completam um símbolo
- [ ] Teste: energia média de símbolo igual a 1 para cada modulação
- [ ] Teste: vizinhos mais próximos diferem em 1 bit (Gray)
- [ ] Teste: ida e volta sem ruído para todos os tamanhos de bloco

**Pronto quando:** todos os testes de constelação e ida e volta passam para as três modulações.

---

## Etapa 7: Validação teórica das modulações
**Objetivo:** provar que a simulação está correta antes de confiar nos resultados.

- [ ] `TheoreticalCurves`: função Q (via `erfc`)
- [ ] `TheoreticalCurves`: BPSK/QPSK, Q(√(2·Eb/N0))
- [ ] `TheoreticalCurves`: 16-QAM com Gray, ≈ 0,75 · Q(√(0,8·Eb/N0))
- [ ] Teste: BPSK em 0 dB e 4 dB dentro de ±15% da teoria
- [ ] Teste: BPSK em 8 dB com tolerância maior ou mais bits
- [ ] Teste: QPSK com a mesma curva do BPSK por bit

**Pronto quando:** BPSK e QPSK batem com a teoria dentro da tolerância. Se não baterem, não avance.

---

## Etapa 8: Pipeline completo e CLI
**Objetivo:** juntar tudo em uma cadeia de transmissão e expor pela linha de comando.

- [ ] `Transmission`: encadeia todas as etapas, escolhendo linha ou modulado por parâmetro
- [ ] `Main` (CLI sem biblioteca extra): `--mensagem`, `--modo`, `--modulacao`, `--ebn0`, `--canal`, `--semente`
- [ ] Saída: texto recebido, BER, número de bits errados e posições
- [ ] Teste: consistência com Eb/N0 de 30 dB, para todas as combinações
- [ ] Teste: com ruído moderado, as posições informadas coincidem com as diferenças entre os bits

**Pronto quando:** a CLI executa o fluxo completo com qualquer combinação e o teste de consistência passa.

---

## Etapa 9: Configuração, estatística e execução de experimentos
**Objetivo:** tornar os experimentos reproduzíveis e automatizados.

- [ ] `ConfigLoader`: lê `config/experimento_base.properties`
- [ ] `Statistics`: média, desvio padrão e IC 95% (t = 2,776 para 5 repetições)
- [ ] `ExperimentRunner`: executa cenário × modulação × Eb/N0 × semente
- [ ] `ResultWriter`: grava uma linha por execução no CSV bruto
- [ ] Teste: mesmas sementes geram CSVs idênticos
- [ ] Teste: estatísticas calculadas à mão batem com o resultado
- [ ] Teste: experimento reduzido gera o número esperado de linhas

**Pronto quando:** um experimento reduzido gera CSV correto e determinístico.

---

## Etapa 10: Experimentos completos
**Objetivo:** executar o plano experimental do trabalho.

- [ ] 8 valores de Eb/N0 (−2 a 12 dB, passo 2 dB)
- [ ] 1.000.000 de bits por execução (justificativa escrita)
- [ ] 5 sementes
- [ ] Todas as modulações, linhas, canais e códigos de canal
- [ ] `scripts/executar_tudo.sh` chamando `java -jar`, com logs salvos

**Pronto quando:** os CSVs completos estão em `resultados/brutos/` e o script roda do zero em uma máquina limpa.

---

## Etapa 11: Gráficos com XChart
**Objetivo:** gerar as curvas BER × SNR a partir dos CSVs.

- [ ] `PlotGenerator` lê os CSVs e calcula média e IC 95% por ponto
- [ ] Curvas BER × Eb/N0 por modulação, com barras de erro e escala log
- [ ] Comparação AWGN × rajadas
- [ ] Curva teórica sobreposta (tracejada)
- [ ] Piso para BER zero, informado na legenda
- [ ] Limite inferior das barras de erro positivo
- [ ] PNG salvo em `resultados/graficos/`

**Pronto quando:** os gráficos são gerados com um único comando e revisados visualmente.

---

## Etapa 12: Análise e discussão
**Objetivo:** transformar os números em interpretação.

- [ ] `docs/tabela_compromisso.md` com eficiência nominal e Eb/N0 para BER-alvo
- [ ] Discussão: quando cada modulação é mais adequada
- [ ] Discussão: impacto do Hamming, do AMI e das rajadas
- [ ] Explicação de divergências entre simulação e teoria (se houver)
- [ ] Opcional: constelações antes e depois do canal (XYChart)

**Pronto quando:** cada gráfico tem uma frase de interpretação e a tabela está preenchida.

---

## Etapa 13: Documentação, reprodutibilidade e vídeo
**Objetivo:** atender aos critérios de repositório e entrega.

- [ ] README completo: arquitetura, parâmetros, instalação, execução, divisão de tarefas, links
- [ ] `docs/arquitetura.md`
- [ ] `docs/parametros.md`
- [ ] `docs/tabela_compromisso.md` (já criado na Etapa 12)
- [ ] Um único comando reproduz CSVs e gráficos
- [ ] Vídeo de até 5 minutos enviado ao GitHub (ou link no README)

**Pronto quando:** outra pessoa clona o repositório, segue o README e reproduz os resultados.

---

## Etapa 14: Apresentação e release

- [ ] Slides de 10 minutos, com divisão de fala entre os dois integrantes
- [ ] Ensaio cronometrado
- [ ] Todos sabem explicar cada classe do projeto
- [ ] Tag `v1.0-final` no GitHub
- [ ] Link no Moodle **antes das 19h de 19/11/2026**