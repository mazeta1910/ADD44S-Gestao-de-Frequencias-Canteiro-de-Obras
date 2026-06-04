package br.edu.utfpr.util;

import java.awt.Font;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.knowm.xchart.AnnotationText;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

/**
 * Exporta dados do benchmark de speed-up para CSV e PNG (gráfico).
 */
public final class ExportadorBenchmark {

    private ExportadorBenchmark() {
    }

    public static Path garantirDiretorioExports() throws IOException {
        Path dir = Paths.get("exports");
        Files.createDirectories(dir);
        return dir;
    }

    public static void exportarSpeedUpCsv(int maxThreads, long[] tempos, double[] speedUps, double[] eficiencias,
            Double[] cpuProcPct, Double[] ramSistemaPct) throws IOException {
        Path dir = garantirDiretorioExports();
        Path arquivo = dir.resolve("speedup.csv");
        try (Writer w = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
            w.write("threads;tempo_ms;speedup;eficiencia_pct;cpu_processo_pct;ram_sistema_pct\n");
            for (int i = 1; i <= maxThreads; i++) {
                String cpu = cpuProcPct[i] != null ? String.format("%.1f", cpuProcPct[i]) : "";
                String ram = ramSistemaPct[i] != null ? String.format("%.1f", ramSistemaPct[i]) : "";
                w.write(String.format(java.util.Locale.US, "%d;%d;%.4f;%.4f;%s;%s%n",
                        i, tempos[i], speedUps[i], eficiencias[i], cpu, ram));
            }
        }
    }

    public static void exportarSpeedUpPng(int maxThreads, double[] speedUps) throws IOException {
        double[] x = new double[maxThreads];
        double[] yMedido = new double[maxThreads];
        double[] yIdeal = new double[maxThreads];
        for (int i = 0; i < maxThreads; i++) {
            x[i] = i + 1;
            yMedido[i] = speedUps[i + 1];
            yIdeal[i] = i + 1;
        }

        XYChart chart = new XYChartBuilder()
                .width(900)
                .height(600)
                .title("Speed-up em função do número de threads")
                .xAxisTitle("Threads")
                .yAxisTitle("Speed-up")
                .build();

        chart.addSeries("Speed-up medido", x, yMedido);
        chart.addSeries("Ideal (linear)", x, yIdeal);
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setYAxisMin(0.0);
        double margemVertical = Math.max(0.24, maxThreads * 0.042);
        chart.getStyler().setYAxisMax(maxThreads + margemVertical + 0.35);
        chart.getStyler().setAnnotationTextFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

        /* Medido: abaixo do ponto e levemente à esquerda; ideal: acima e à direita — evita sobreposição no espaço entre as curvas. */
        double deslocX = 0.11;
        for (int i = 0; i < maxThreads; i++) {
            double yRotuloMedido = Math.max(0.06, yMedido[i] - margemVertical);
            chart.addAnnotation(new AnnotationText(
                    String.format(Locale.US, "%.2f", yMedido[i]),
                    x[i] - deslocX, yRotuloMedido, false));
            chart.addAnnotation(new AnnotationText(
                    String.format(Locale.US, "%.1f", yIdeal[i]),
                    x[i] + deslocX, yIdeal[i] + margemVertical, false));
        }

        Path dir = garantirDiretorioExports();
        BitmapEncoder.saveBitmap(chart, dir.resolve("speedup.png").toString(), BitmapFormat.PNG);
    }
}
