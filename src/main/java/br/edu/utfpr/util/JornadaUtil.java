package br.edu.utfpr.util;

import br.edu.utfpr.model.RegistroPonto;
import br.edu.utfpr.model.TipoContrato;
import br.edu.utfpr.model.Trabalhador;

import java.time.Duration;
import java.time.LocalTime;

public final class JornadaUtil {

    private JornadaUtil() {
    }

    public static long calcularMinutosTrabalhados(RegistroPonto registro, LocalTime referencia) {
        if (registro.getHoraEntrada() == null) {
            return 0;
        }

        LocalTime horaEntrada = registro.getHoraEntrada();
        LocalTime horaSaidaIntervalo = registro.getHoraSaidaIntervalo();
        LocalTime horaRetornoIntervalo = registro.getHoraRetornoIntervalo();
        LocalTime horaSaida = registro.getHoraSaida();
        LocalTime fim = horaSaida != null ? horaSaida : referencia;

        long minutos = 0;

        if (horaSaidaIntervalo != null) {
            minutos += Duration.between(horaEntrada, horaSaidaIntervalo).toMinutes();
            if (horaRetornoIntervalo != null && !fim.isBefore(horaRetornoIntervalo)) {
                minutos += Duration.between(horaRetornoIntervalo, fim).toMinutes();
            }
        } else if (!fim.isBefore(horaEntrada)) {
            minutos += Duration.between(horaEntrada, fim).toMinutes();
        }

        return Math.max(0, minutos);
    }

    public static long resolverMetaDiaria(Trabalhador trabalhador) {
        return TipoContrato.fromString(trabalhador.getTipoContrato()).getMetaMinutosDiarios();
    }

    public static long calcularSaldo(RegistroPonto registro, Trabalhador trabalhador, LocalTime referencia) {
        return calcularMinutosTrabalhados(registro, referencia) - resolverMetaDiaria(trabalhador);
    }

    public static String formatarDuracao(long minutos) {
        long horas = minutos / 60;
        long mins = minutos % 60;
        return String.format("%dh %02dm", horas, mins);
    }

    public static String formatarSaldo(long saldoMinutos) {
        if (saldoMinutos == 0) {
            return "0h 00m (meta atingida)";
        }
        if (saldoMinutos > 0) {
            return "+" + formatarDuracao(saldoMinutos) + " acima da meta";
        }
        return "-" + formatarDuracao(Math.abs(saldoMinutos)) + " para completar a meta";
    }

    public static String formatarSaldoCurto(long saldoMinutos) {
        if (saldoMinutos == 0) {
            return "0h00";
        }
        String sinal = saldoMinutos > 0 ? "+" : "-";
        long abs = Math.abs(saldoMinutos);
        return String.format("%s%dh%02d", sinal, abs / 60, abs % 60);
    }

    public static String montarRespostaEstado(String estado, RegistroPonto registro, Trabalhador trabalhador, LocalTime referencia) {
        long trabalhados = calcularMinutosTrabalhados(registro, referencia);
        long meta = resolverMetaDiaria(trabalhador);
        long saldo = trabalhados - meta;
        String contrato = TipoContrato.fromString(trabalhador.getTipoContrato()).name();

        return String.format(
                "ESTADO;%s;TRAB;%d;META;%d;SALDO;%d;CONTRATO;%s",
                estado, trabalhados, meta, saldo, contrato
        );
    }
}
