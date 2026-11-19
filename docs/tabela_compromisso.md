# Tabela de compromisso

Gerada por `TradeoffTable` a partir do CSV de resultados. Eb/N0 em dB, por bit de informação. Os valores simulados são interpolados linearmente em log10(BER) entre os pontos medidos (passo de 2 dB). "> X": a BER-alvo não foi atingida até o maior Eb/N0 simulado. "< X": já estava abaixo dela no menor. A coluna teórica existe só para AWGN sem código.

| Esquema | Canal | Código | Eficiência nominal (bit/s/Hz) | Eb/N0 p/ BER 10⁻³ (simulado) | (teoria) | Eb/N0 p/ BER 10⁻⁴ (simulado) | (teoria) |
|---|---|---|---|---|---|---|---|
| BPSK | AWGN | nenhum | 1,000 | 6,7 | 6,8 | 8,3 | 8,4 |
| BPSK | AWGN | Hamming(7,4) | 0,571 | 6,6 | — | 8,1 * | — |
| BPSK | RAJADAS | nenhum | 1,000 | 10,2 | — | > 12 | — |
| BPSK | RAJADAS | Hamming(7,4) | 0,571 | 10,2 | — | > 12 | — |
| QPSK | AWGN | nenhum | 2,000 | 6,7 | 6,8 | 8,3 | 8,4 |
| QPSK | AWGN | Hamming(7,4) | 1,143 | 6,6 | — | 8,1 * | — |
| QPSK | RAJADAS | nenhum | 2,000 | 10,2 | — | > 12 | — |
| QPSK | RAJADAS | Hamming(7,4) | 1,143 | 10,3 | — | > 12 | — |
| 16-QAM | AWGN | nenhum | 4,000 | 10,4 | 10,5 | > 12 | 12,2 |
| 16-QAM | AWGN | Hamming(7,4) | 2,286 | 10,1 | — | 11,5 | — |
| 16-QAM | RAJADAS | nenhum | 4,000 | > 12 | — | > 12 | — |
| 16-QAM | RAJADAS | Hamming(7,4) | 2,286 | > 12 | — | > 12 | — |
| AMI | AWGN | nenhum | 1,000 | 10,1 | 10,1 | 11,6 | 11,6 |
| AMI | AWGN | Hamming(7,4) | 0,571 | 10,2 | — | 11,4 | — |
| AMI | RAJADAS | nenhum | 1,000 | > 12 | — | > 12 | — |
| AMI | RAJADAS | Hamming(7,4) | 0,571 | > 12 | — | > 12 | — |
| Manchester | AWGN | nenhum | 0,500 | 6,7 | 6,8 | 8,3 | 8,4 |
| Manchester | AWGN | Hamming(7,4) | 0,286 | 6,6 | — | 8,1 * | — |
| Manchester | RAJADAS | nenhum | 0,500 | 10,1 | — | > 12 | — |
| Manchester | RAJADAS | Hamming(7,4) | 0,286 | 9,9 | — | > 12 | — |

\* Um dos pontos usados na interpolação teve 0 erros em alguma semente (entrou na média com o piso 1/N).

## Diferenças em dB para BER 10⁻³

Positivo: precisa de mais Eb/N0 (piora). Negativo: precisa de menos (ganho). "—" quando algum dos dois valores não foi atingido na faixa simulada.

| Esquema | Hamming − sem código (AWGN) | Hamming − sem código (rajadas) | Rajadas − AWGN (sem código) |
|---|---|---|---|
| BPSK | -0,1 | +0,0 | +3,5 |
| QPSK | -0,1 | +0,1 | +3,5 |
| 16-QAM | -0,4 | — | — |
| AMI | +0,1 | — | — |
| Manchester | -0,1 | -0,2 | +3,4 |
