# Parâmetros e convenções

## Eb/N0 e sigma

- O Eb/N0 do eixo é **nominal**, referido ao bit de informação. Com Hamming(7,4), o `SnrConverter` considera a taxa R = 4/7: Es/N0 = Eb/N0 + 10·log10(k·R).
- O sigma é calculado a partir do Eb/N0 nominal. O AWGN e o estado bom do canal de rajadas usam o mesmo sigma em cada ponto.
- `TransmissionResult.sigma()` é o sigma do estado bom (nominal). **Não** é a potência média do ruído.

## Canal de rajadas (Gilbert-Elliott)

- Dois estados: bom (ruído com sigma) e ruim (ruído com sigma × fator, fator ≥ 1).
- `p` é a probabilidade, por amostra, de entrar no estado ruim. `r` é a probabilidade, por amostra, de sair dele.
- Fração esperada no estado ruim em regime: π = p / (p + r). Com p = 0,01 e r = 0,1, π ≈ 0,091.
- Duração média de uma rajada: 1/r amostras.
- Potência média do ruído em relação ao AWGN: (1 − π) + π·fator².
  - Fator 2: 0,909 + 0,091·4 ≈ 1,27 (≈ +1,0 dB). Padrão do simulador.
  - Fator 5: 0,909 + 0,091·25 ≈ 3,18 (≈ +5,0 dB). Descartado pelo piloto.
- Consequência: o Eb/N0 nominal de rajadas não corresponde ao Eb/N0 efetivo. A comparação com o AWGN é feita no mesmo Eb/N0 nominal, e a diferença entre as curvas é a degradação causada pelas rajadas.
- A contagem é **por amostra do canal**, não por bit. Amostras por bit ou símbolo:

| Esquema | Amostras por bit/símbolo | Bits por símbolo |
|---|---|---|
| AMI | 1 | 1 |
| Manchester | 2 | 1 |
| BPSK | 1 | 1 |
| QPSK | 1 | 2 |
| 16-QAM | 1 | 4 |

- Com r = 0,1 (duração média de 10 amostras), uma rajada atinge cerca de 10 bits em AMI e BPSK, 5 em Manchester, 20 em QPSK e 40 em 16-QAM. Com os mesmos p e r, as rajadas corrompem mais bits nas modulações de ordem superior. Isso deve ser declarado ao comparar os resultados.
- O estado do canal é mantido entre chamadas. `Transmission` cria um canal novo a cada execução, para que execuções sejam independentes.

## Escolha do fator de degradação

O padrão do simulador (`ConfigTransmission.FATOR_PADRAO`) e o valor de `rajadas.fator` nos arquivos de experimento são **2,0**.

O fator 5,0 foi testado no piloto e descartado por saturar: a BER das rajadas cai pouco entre 8 e 12 dB, e o canal deixa de ser "ruim em rajadas" para ser "ruim sempre".

**Piso de BER do estado ruim.** Em altas razões Eb/N0, a BER do canal é dominada pela fração no estado ruim, com a BER do estado ruim dada por Q(1/(fator·σ)) para BPSK:

piso ≈ π · Q(1 / (fator · σ))

Em 12 dB, σ ≈ 0,178:

- Fator 5: 0,091 · Q(1/0,89) ≈ 0,012. Simulado: 1,21·10⁻².
- Fator 2: 0,091 · Q(1/0,355) ≈ 2,2·10⁻⁴. Simulado: 2,6·10⁻⁴.

**Razões no piloto (BPSK, sem código, 10⁵ bits, 2 sementes):**

| Eb/N0 | AWGN | Rajadas (fator 2) | Razão (fator 2) | Rajadas (fator 5) | Razão (fator 5) |
|---|---|---|---|---|---|
| 4 dB | 1,28·10⁻² | 2,38·10⁻² | 1,9× | 4,20·10⁻² | 3,3× |
| 6 dB | 2,42·10⁻³ | 9,65·10⁻³ | 4,0× | 2,89·10⁻² | 11,9× |
| 8 dB | 2,20·10⁻⁴ | 3,94·10⁻³ | 17,9× | 2,25·10⁻² | 102× |
| 10 dB | 1,50·10⁻⁵ | 1,26·10⁻³ | 83,7× | 1,72·10⁻² | 1.150× |

Com fator 2, a degradação é visível de 4 a 10 dB sem saturar. Os resultados completos estão em `docs/piloto_fator2.txt` e `docs/piloto_fator5.txt`.

## Eficiência espectral nominal

- Convenção: banda medida do zero ao primeiro nulo, com B = Rs (taxa de símbolos ou de amostras).
- AMI: 1 bit/s/Hz. Manchester: 0,5 bit/s/Hz. BPSK: 1. QPSK: 2. 16-QAM: 4.
- Com código de canal, multiplica-se pela taxa do código. Para Hamming(7,4), R = 4/7 ≈ 0,57 (por exemplo, AMI + Hamming = 0,57 bit/s/Hz).

## Código de canal

- Hamming(7,4): corrige 1 erro por bloco de 7 bits. Decisão abrupta (hard decision).
- Padding: zeros acrescentados ao último bloco de 4 bits de informação. Os zeros são descartados na decodificação.
- **Cruzamento com o caso sem código** (piloto, fator 2, AWGN, contagem de erros):
  - BPSK e QPSK: o código passa a ser melhor entre 6 e 8 dB. Em 6 dB, BPSK tem 483 erros sem código e 500 com código. Em 8 dB, tem 44 e 23.
  - Manchester: o código passa a ser melhor entre 4 e 6 dB (2439 contra 3099 em 4 dB; 465 contra 451 em 6 dB).
  - 16-QAM: o cruzamento fica entre 6 e 8 dB (5510 contra 5767 em 6 dB; 1822 contra 1546 em 8 dB).
  - AMI: o código é pior até 10 dB (251 contra 286 erros). Em 12 dB, passa a ser melhor (16 contra 2). O cruzamento fica entre 10 e 12 dB.
  - A piora de AMI com decisão abrupta é esperada. A aproximação de Hamming (≈ 9·p² para o erro de bloco, com p a probabilidade de erro no canal) dá cruzamento perto de 11 dB para AMI. Isso entra na análise como discussão, não como defeito.
- Em 10 e 12 dB, com 10⁵ bits, as contagens são muito baixas (0 a 3 erros em vários casos). Por isso a comparação de Hamming usa contagens e margem de 3 desvios de Poisson, não médias de BER.

## Posições dos bits errados

- Índices base 0, nos bits da mensagem **após** a decodificação do Hamming, se houver.
- Não se referem aos bits transmitidos no canal.

## Mapeamentos

- Bits em ordem MSB primeiro.
- BPSK: 0 → −1, 1 → +1, normalização unitária.
- QPSK: 1º bit → componente I, 2º bit → componente Q; 0 negativo, 1 positivo; escala 1/√2.
- 16-QAM (Gray): por eixo, 00 → −3, 01 → −1, 11 → +1, 10 → +3; escala 1/√10.
- AMI: 0 → 0; 1 alterna entre +1 e −1, começando em +1. Decisão por |x| > 0,5.
- Manchester: 1 → (+1, −1); 0 → (−1, +1). Decisão pela diferença r1 − r2 entre as metades do bit.

## Semente

- Mensagem: `DataGenerator.bitsAleatorios(bits, semente)`. Com a mesma semente, todos os esquemas recebem a mesma sequência de bits (comparação pareada).
- Canal: semente + 1000 (`DESLOCAMENTO_SEMENTE_CANAL`). Com a mesma semente, o gerador de ruído começa no mesmo ponto, mas esquemas diferentes consomem números aleatórios de forma diferente. Por isso a mesma semente **não** garante o mesmo ruído entre esquemas.
- Cada execução é reproduzível: mesma configuração e mesma semente geram o mesmo CSV.

## Verificação dos resultados

O script `scripts/verificar_piloto.sh` (originalmente `verificar_piloto.ps1`) confere, para cada CSV:

- formato, contagem de linhas e ausência de combinações repetidas;
- consistência de BER e de `bits_mensagem`;
- preenchimento de `p`, `r` e `fator` apenas nas rajadas;
- sigma das rajadas igual ao do AWGN no mesmo ponto;
- BPSK sem código no AWGN dentro de 15% de Q(√(2·Eb/N0)) em 0, 2, 4 e 6 dB;
- Hamming não significativamente pior que o caso sem código em 10 e 12 dB, para BPSK, QPSK, 16-QAM e Manchester;
- rajadas não melhores que o AWGN onde a BER do AWGN é maior que 10⁻³.

As saídas dos dois pilotos estão em `docs/piloto_fator2.txt` e `docs/piloto_fator5.txt`. Ambos passaram nas verificações técnicas.

## Configurações experimentais

| Arquivo | Uso | Bits | Sementes | Execuções | Fator |
|---|---|---|---|---|---|
| `config/experimento_teste.properties` | fumaça e determinismo | 10⁴ | 2 | 32 | 2,0 |
| `config/experimento_piloto_f2.properties` | escolha de parâmetros (escolhido) | 10⁵ | 2 | 320 | 2,0 |
| `config/experimento_piloto.properties` | escolha de parâmetros (descartado) | 10⁵ | 2 | 320 | 5,0 |
| `config/experimento_base.properties` | experimento final | 10⁶ | 5 | 800 | 2,0 |

Grade de Eb/N0 do experimento final: −2 a 12 dB, passo 2 dB (8 valores).

Com duas sementes, o IC 95% do piloto é muito largo (t = 12,706). Ele serve para escolher o fator, não para os resultados finais.

## Tempo de execução

- O tempo do piloto não foi registrado no log.
- Experimento final (800 execuções, 10⁶ bits): ≈ 4 minutos (229 s no log `resultados/logs/experimento_base_20261118_193739.log`). Roda em uma única sessão.
