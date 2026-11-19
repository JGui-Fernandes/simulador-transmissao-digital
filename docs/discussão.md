# Análise e discussão dos resultados

Todos os valores de Eb/N0 são por bit de informação. Os números citados vêm de
`docs/tabela_compromisso.md` (BER-alvo 10⁻³, salvo indicação).

## Interpretação das figuras

**Figura 1 (modulações em AWGN, sem código).** BPSK e QPSK ficam sobrepostas e sobre
a curva teórica Q(√(2·Eb/N0)): as duas atingem BER 10⁻³ em 6,7 dB (teoria: 6,8 dB).
O QPSK transmite o dobro de bits por símbolo sem piorar a BER por bit, porque
equivale a dois BPSK em I e Q. O 16-QAM precisa de 10,4 dB (teoria: 10,5 dB), ou
seja, 3,7 dB a mais que o BPSK, porque seus pontos estão mais próximos uns dos
outros na constelação.

**Figura 2 (AWGN × rajadas).** Com rajadas, a BER deixa de cair rapidamente em Eb/N0
alto: o erro passa a ser dominado pelos ≈ 9% do tempo no estado ruim, em que a SNR é
6 dB menor (σ × 2). Para BER 10⁻³, a penalidade é de 3,5 dB no BPSK e no QPSK e de
3,4 dB no Manchester; 16-QAM e AMI nem chegam a 10⁻³ até 12 dB. Nenhum esquema
atinge BER 10⁻⁴ com rajadas na faixa simulada, enquanto em AWGN o BPSK chega lá em
8,3 dB. A comparação usa Eb/N0 nominal: o estado bom recebe o mesmo σ do AWGN e o
ruim, σ × 2, de modo que a potência média do ruído no canal de rajadas é ≈ 1,27 vez
a do AWGN (+1,05 dB). Cerca de 1 dB da penalidade vem desse ruído extra; o restante
(≈ 2,4 dB), da concentração do ruído em rajadas.

**Figura 3 (Hamming nas modulações, AWGN).** Em Eb/N0 baixo, o Hamming(7,4) piora a
BER: cada bit transmitido carrega só 4/7 da energia de um bit de informação, e o
código não corrige erros suficientes para compensar. As curvas se cruzam perto de
6 dB e, a partir daí, o código passa a ganhar, mas pouco: 0,1 dB em BER 10⁻³ e
0,2 dB em 10⁻⁴ no BPSK e no QPSK, ao custo de reduzir a eficiência para 4/7. No
16-QAM o ganho é maior: 0,4 dB em 10⁻³, e com o código a BER 10⁻⁴ passa a ser
atingida em 11,5 dB, o que não acontece sem ele até 12 dB.

**Figura 4 (códigos de linha, AWGN).** O Manchester segue a mesma curva do BPSK
(6,7 dB para 10⁻³), porque a decisão pela diferença entre as metades usa toda a
energia do bit. O AMI precisa de 10,1 dB, ou seja, 3,4 dB a mais (teoria: 10,1 dB):
seus níveis são 0 e ±1, então a distância até o limiar de decisão é metade da do
Manchester, e o bit 0 pode errar para os dois lados. Com Hamming, o AMI ainda perde
0,1 dB em 10⁻³ e só passa a ganhar em 10⁻⁴ (0,2 dB).

## Quando cada modulação é mais adequada

- **BPSK:** apresenta boa robustez contra ruído, sendo adequado para situações em que a confiabilidade é 
  mais importante que a eficiência espectral. Nos resultados, apresentou a mesma BER do QPSK, mas com metade 
  da eficiência espectral.
- **QPSK:** a mesma BER por bit do BPSK (6,7 dB para 10⁻³ nos dois) com o dobro da
  eficiência (2 bit/s/Hz). É a escolha padrão quando a SNR é moderada; em nossos
  resultados, domina o BPSK.
- **16-QAM:** 4 bit/s/Hz, mas exige 3,7 dB a mais que o QPSK para a mesma BER.
  Adequada para enlaces com SNR alta e banda escassa, como cabo ou Wi-Fi perto do
  ponto de acesso. Sistemas reais usam modulação adaptativa: trocam de QPSK para
  16-QAM conforme a SNR melhora.

## Impacto do Hamming, do AMI e das rajadas

**Hamming(7,4).** Corrige 1 erro por bloco de 7 bits e custa 3/7 da taxa. Com
decisão abrupta (hard decision), o ganho em AWGN é pequeno (0,1 a 0,4 dB em 10⁻³) e
só aparece em Eb/N0 alto. Com rajadas, o ganho praticamente desaparece (entre
−0,2 e +0,1 dB em 10⁻³): a duração média de uma rajada (1/r = 10 amostras) coloca
vários erros no mesmo bloco de 7 bits, e o decodificador "corrige" para a palavra
errada. Ou seja, o código perde 3/7 da taxa sem trazer benefício. A solução clássica
é o entrelaçamento (interleaving), que espalha os bits de cada bloco no tempo; ele
não foi implementado.

**AMI × Manchester.** O AMI ocupa metade da banda do Manchester (1 contra
0,5 bit/s/Hz) e não tem componente DC, mas é 3,4 dB pior em BER 10⁻³ (3,3 dB em
10⁻⁴) e perde o sincronismo em sequências longas de zeros. O Manchester tem transição
no meio de todo bit, o que garante a recuperação do relógio, ao custo de dobrar a
banda.

**Rajadas.** Custam cerca de 3,5 dB em BER 10⁻³ e impedem que qualquer esquema
chegue a 10⁻⁴ até 12 dB. Além de aumentar a BER, concentram os erros no tempo, o que
anula o benefício de códigos projetados para erros isolados, como o Hamming.

## Divergências entre simulação e teoria

- **Concordância em AWGN:** sem código, os valores simulados de Eb/N0 para BER 10⁻³
  e 10⁻⁴ ficaram a no máximo 0,1 dB da teoria em BPSK, QPSK, 16-QAM, AMI e
  Manchester. A diferença vem da interpolação: log(BER) é uma função côncava do
  Eb/N0, então a reta entre dois pontos medidos fica abaixo da curva real e atinge a
  BER-alvo um pouco antes, o que torna os valores interpolados ligeiramente otimistas.
- **Piso 1/N:** com 10⁶ bits por semente, a menor BER mensurável é 10⁻⁶. Pontos em
  que alguma semente teve 0 erros (losangos cinzas nas figuras, asterisco na tabela)
  usam o piso e são um limite superior, não uma medida. Por isso as curvas simuladas
  "param" acima da teórica em Eb/N0 alto, e os valores 8,1 * do Hamming em 10⁻⁴ devem
  ser lidos com cautela.
- **Barras de erro cortadas:** a barra é limitada para não descer abaixo do piso
  (escala log); o corte é simétrico, então também encurta a parte de cima.
- **16-QAM:** a curva teórica é a aproximação 0,75·Q(√(0,8·Eb/N0)), que considera só
  erros para o vizinho mais próximo; é menos precisa em Eb/N0 baixo. Na faixa da
  tabela, a simulação ficou a 0,1 dB dela.
- **AMI:** a teórica 1,5·Q(√(Eb/N0)) despreza o caso raro de um ±1 cruzar até o nível
  oposto, o que é desprezível na faixa simulada; simulação e teoria coincidiram
  (10,1 dB em 10⁻³ e 11,6 dB em 10⁻⁴).
- **Rajadas e Hamming:** não há curva teórica fechada no projeto; os resultados
  foram avaliados por comparação com o AWGN.
- **Normalização das rajadas:** o comentário no `Transmission` menciona a redução do σ
  do estado bom para igualar o Eb/N0 médio, mas o código usa o σ nominal. Os
  resultados de rajadas devem ser lidos como comparação em Eb/N0 nominal (ruído médio
  +1,05 dB em relação ao AWGN), como descrito na Figura 2.
- **Interpolação da tabela:** os valores de Eb/N0 para BER-alvo vêm de interpolação
  entre pontos espaçados de 2 dB, com precisão de alguns décimos de dB.