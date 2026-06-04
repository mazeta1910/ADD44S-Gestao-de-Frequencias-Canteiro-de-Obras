package br.edu.utfpr.service;

import java.util.Arrays;

public class ProcessadorFolhaPagamento {

    /** Carga padrão (benchmark completo). */
    public static final int TOTAL_REGISTROS = 300_000_000;

    private static final int MESES_ANO = 12;
    private static final long SEED_BASE = 0x9E3779B97F4A7C15L;

    private final int totalRegistros;
    private final double[] salariosBase;
    private final double[] salariosLiquidos;

    public ProcessadorFolhaPagamento() {
        this(TOTAL_REGISTROS);
    }

    public ProcessadorFolhaPagamento(int totalRegistros) {
        if (totalRegistros <= 0) {
            throw new IllegalArgumentException("totalRegistros deve ser positivo.");
        }
        this.totalRegistros = totalRegistros;
        salariosBase = new double[totalRegistros];
        salariosLiquidos = new double[totalRegistros];

        for (int i = 0; i < totalRegistros; i++) {
            salariosBase[i] = 1500 + (Math.random() * 7000);
        }
    }

    public int getTotalRegistros() {
        return totalRegistros;
    }

    private long hashDeterministico(int empregadoId, int mes, int sal) {
        long x = SEED_BASE ^ ((long) empregadoId * 0xBF58476D1CE4E5B9L);
        x ^= ((long) mes + 1L) * 0x94D049BB133111EBL;
        x ^= ((long) sal + 11L) * 0xD6E8FEB86659FD93L;
        x ^= (x >>> 30);
        x *= 0xBF58476D1CE4E5B9L;
        x ^= (x >>> 27);
        x *= 0x94D049BB133111EBL;
        x ^= (x >>> 31);
        return x;
    }

    private double randomDeterministico01(int empregadoId, int mes, int sal) {
        long bits = hashDeterministico(empregadoId, mes, sal) & ((1L << 53) - 1);
        return bits / (double) (1L << 53);
    }

    private int gerarFaltasNoMes(int empregadoId, int mes) {
        double sorteio = randomDeterministico01(empregadoId, mes, 1);
        if (sorteio < 0.50) {
            return 0;
        }
        if (sorteio < 0.80) {
            return 1;
        }
        if (sorteio < 0.95) {
            return 2;
        }
        return 3;
    }

    /**
     * Dias de afastamento no mês (0 = nenhum). Referência: mês com 30 dias.
     * Quando há afastamento, a quantidade de dias é sorteada de forma determinística.
     */

    private int gerarDiasAfastamentoNoMes(int empregadoId, int mes) {
        if (randomDeterministico01(empregadoId, mes, 3) >= 0.015) {
            return 0;
        }
        int dias = 1 + (int) (randomDeterministico01(empregadoId, mes, 7) * 25);
        return Math.min(dias, 30);
    }

    private void calcularFolhaAnual(int index) {
        int empregadoId = index + 1;
        double salarioBase = salariosBase[index];
        double liquidoAnual = 0.0;
        boolean demitido = false;

        for (int mes = 0; mes < MESES_ANO && !demitido; mes++) {
            double fatorSazonal = 0.97 + (randomDeterministico01(empregadoId, mes, 2) * 0.06);
            double salarioMes = salarioBase * fatorSazonal;

            int faltasMes = gerarFaltasNoMes(empregadoId, mes);
            int diasAfastamento = gerarDiasAfastamentoNoMes(empregadoId, mes);
            boolean trabalhouFeriado = randomDeterministico01(empregadoId, mes, 4) < 0.10;
            boolean fezHoraExtra = randomDeterministico01(empregadoId, mes, 5) < 0.18;
            boolean demissaoNoMes = randomDeterministico01(empregadoId, mes, 6) < 0.004;

            // Desconto por faltas: valor de 1 dia = salário mensal / 30
            double valorDia = salarioMes / 30.0;
            double descontoFaltas = faltasMes * valorDia;

            // Afastamento proporcional aos dias: (30−d)×100% + d×60% do valor diário, sobre base de 30 dias
            double fatorAfastamento = (30.0 - 0.40 * diasAfastamento) / 30.0;
            salarioMes *= fatorAfastamento;

            double adicionalHoraExtra = fezHoraExtra ? salarioMes * 0.12 : 0.0;
            double adicionalFeriado = trabalhouFeriado ? salarioMes * 0.08 : 0.0;

            double inss = salarioMes * 0.11;
            double irrf = Math.max(0.0, (salarioMes - inss) * 0.275);
            double fgts = salarioMes * 0.08;
            double periculosidade = Math.pow(salarioMes, 1.05) / 100.0;

            double liquidoMes = salarioMes - inss - irrf - descontoFaltas + adicionalHoraExtra + adicionalFeriado + periculosidade;

            if (mes == 11) {
                liquidoMes += (salarioBase * 0.50);
            }

            if (demissaoNoMes) {
                double multaRescisoria = fgts * 0.40;
                liquidoMes -= multaRescisoria;
                demitido = true;
            }

            liquidoAnual += liquidoMes;
        }

        salariosLiquidos[index] = liquidoAnual;
    }

    public long executarSerial() {
        long inicio = System.currentTimeMillis();

        for (int i = 0; i < totalRegistros; i++) {
            calcularFolhaAnual(i);
        }

        long fim = System.currentTimeMillis();
        return (fim - inicio);
    }

    public long executarParaleloDuasThreads() {
        long inicio = System.currentTimeMillis();

        int metade = totalRegistros / 2;

        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < metade; i++) {
                calcularFolhaAnual(i);
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = metade; i < totalRegistros; i++) {
                calcularFolhaAnual(i);
            }
        });

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long fim = System.currentTimeMillis();
        return (fim - inicio);
    }

    public void imprimirEstatisticas() {
        double max = Double.MIN_VALUE;
        double min = Double.MAX_VALUE;
        double soma = 0;

        for (int i = 0; i < totalRegistros; i++) {
            double salario = salariosLiquidos[i];
            if (salario > max) {
                max = salario;
            }
            if (salario < min) {
                min = salario;
            }
            soma += salario;
        }

        double media = soma / totalRegistros;

        System.out.println("--- Resumo estatístico do líquido anual (todos os registros em memória) ---");
        System.out.printf("Maior líquido anual: R$ %.2f%n", max);
        System.out.printf("Menor líquido anual: R$ %.2f%n", min);
        System.out.printf("Média líquida anual: R$ %.2f%n", media);
    }

    public long executarParaleloVariavel(int numThreads) {
        long inicio = System.currentTimeMillis();
        Thread[] threads = new Thread[numThreads];

        int tamanhoLote = totalRegistros / numThreads;

        for (int i = 0; i < numThreads; i++) {
            final int inicioLote = i * tamanhoLote;
            final int fimLote = (i == numThreads - 1) ? totalRegistros : (i + 1) * tamanhoLote;

            threads[i] = new Thread(() -> {
                for (int j = inicioLote; j < fimLote; j++) {
                    calcularFolhaAnual(j);
                }
            });
            threads[i].start();
        }

        try {
            for (int i = 0; i < numThreads; i++) {
                threads[i].join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long fim = System.currentTimeMillis();
        return (fim - inicio);
    }

    /**
     * @param imprimirResumoTempos se true, imprime total e média por lote após a tabela (útil para uma única execução).
     */

    public long executarSerialComTabela(int qtdLotes, boolean imprimirResumoTempos) {
        long inicioTotal = System.currentTimeMillis();

        System.out.println("+------+-------------------------------+--------------------+");
        System.out.println("| Lote | Registros Processados         | Tempo Parcial (ms) |");
        System.out.println("+------+-------------------------------+--------------------+");

        int tamanhoLote = totalRegistros / qtdLotes;

        for (int i = 0; i < qtdLotes; i++) {
            long inicioLote = System.currentTimeMillis();

            int inicioIndex = i * tamanhoLote;
            int fimIndex = (i == qtdLotes - 1) ? totalRegistros : inicioIndex + tamanhoLote;

            for (int j = inicioIndex; j < fimIndex; j++) {
                calcularFolhaAnual(j);
            }

            long fimLote = System.currentTimeMillis();
            long tempoParcial = fimLote - inicioLote;

            System.out.printf("| %-4d | %-,13d a %-,13d | %-18d |\n", (i + 1), inicioIndex, (fimIndex - 1), tempoParcial);
        }

        long fimTotal = System.currentTimeMillis();
        long tempoTotal = fimTotal - inicioTotal;
        System.out.println("+------+-------------------------------+--------------------+");
        if (imprimirResumoTempos) {
            long mediaPorLote = tempoTotal / Math.max(1, qtdLotes);
            System.out.printf("TOTAL DA EXECUÇÃO SERIAL (%,d lotes): %,d ms%n", qtdLotes, tempoTotal);
            System.out.printf("MÉDIA POR LOTE: %,d ms%n", mediaPorLote);
        }

        return tempoTotal;
    }

    public void imprimirCuriosidadesEventosAmostrais(int tamanhoAmostra, int topN) {
        int limite = Math.min(tamanhoAmostra, totalRegistros);
        if (limite <= 0 || topN <= 0) {
            return;
        }

        int[] topFaltasIds = new int[topN];
        int[] topFaltasValores = new int[topN];
        int[] topDescontosIds = new int[topN];
        double[] topDescontosValores = new double[topN];
        Arrays.fill(topFaltasIds, -1);
        Arrays.fill(topDescontosIds, -1);

        int totalFaltasAmostra = 0;
        int mesesComAfastamentoAmostra = 0;
        int totalDiasAfastamentoAmostra = 0;
        int totalDemissoesAmostra = 0;

        for (int index = 0; index < limite; index++) {
            int empregadoId = index + 1;
            double salarioBase = salariosBase[index];
            int faltasAnuais = 0;
            double descontoTotal = 0.0;
            boolean demitido = false;

            for (int mes = 0; mes < MESES_ANO && !demitido; mes++) {
                double fatorSazonal = 0.97 + (randomDeterministico01(empregadoId, mes, 2) * 0.06);
                double salarioMes = salarioBase * fatorSazonal;
                int faltasMes = gerarFaltasNoMes(empregadoId, mes);
                int diasAfast = gerarDiasAfastamentoNoMes(empregadoId, mes);
                boolean demissaoNoMes = randomDeterministico01(empregadoId, mes, 6) < 0.004;

                if (diasAfast > 0) {
                    mesesComAfastamentoAmostra++;
                    totalDiasAfastamentoAmostra += diasAfast;
                }
                if (demissaoNoMes) {
                    totalDemissoesAmostra++;
                    demitido = true;
                }

                double descontoFaltas = (salarioMes / 30.0) * faltasMes;
                faltasAnuais += faltasMes;
                descontoTotal += descontoFaltas;
            }

            totalFaltasAmostra += faltasAnuais;

            for (int k = 0; k < topN; k++) {
                if (faltasAnuais > topFaltasValores[k]) {
                    for (int m = topN - 1; m > k; m--) {
                        topFaltasValores[m] = topFaltasValores[m - 1];
                        topFaltasIds[m] = topFaltasIds[m - 1];
                    }
                    topFaltasValores[k] = faltasAnuais;
                    topFaltasIds[k] = empregadoId;
                    break;
                }
            }

            for (int k = 0; k < topN; k++) {
                if (descontoTotal > topDescontosValores[k]) {
                    for (int m = topN - 1; m > k; m--) {
                        topDescontosValores[m] = topDescontosValores[m - 1];
                        topDescontosIds[m] = topDescontosIds[m - 1];
                    }
                    topDescontosValores[k] = descontoTotal;
                    topDescontosIds[k] = empregadoId;
                    break;
                }
            }
        }

        System.out.printf("%n--- Indicadores de eventos (amostra de %,d empregados) ---%n", limite);
        System.out.printf("Faltas totais na amostra: %,d%n", totalFaltasAmostra);
        System.out.printf("Meses com algum afastamento (na amostra): %,d | Soma de dias afastados: %,d%n",
                mesesComAfastamentoAmostra, totalDiasAfastamentoAmostra);
        System.out.printf("Demissões simuladas no ano: %,d%n", totalDemissoesAmostra);

        System.out.printf("%nTop %d IDs com mais faltas:%n", topN);
        for (int i = 0; i < topN; i++) {
            if (topFaltasIds[i] > 0) {
                System.out.printf("  %d) ID %,d -> %d faltas%n", (i + 1), topFaltasIds[i], topFaltasValores[i]);
            }
        }

        System.out.printf("%nTop %d IDs com maior desconto por faltas (R$):%n", topN);
        for (int i = 0; i < topN; i++) {
            if (topDescontosIds[i] > 0) {
                System.out.printf("  %d) ID %,d -> R$ %,.2f%n", (i + 1), topDescontosIds[i], topDescontosValores[i]);
            }
        }
    }
}
