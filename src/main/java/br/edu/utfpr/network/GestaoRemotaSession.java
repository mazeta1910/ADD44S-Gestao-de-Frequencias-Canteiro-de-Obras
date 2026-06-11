package br.edu.utfpr.network;

import br.edu.utfpr.MenuConsoleSimplificado;
import br.edu.utfpr.util.JPAUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executa o menu de gestao corporativa no servidor e encaminha entrada/saida
 * pela conexao TCP ja autenticada — evita exigir PostgreSQL acessivel na rede.
 */
public final class GestaoRemotaSession {

    private static final String MARCADOR_LINHA = "L|";
    private static final String MARCADOR_PARCIAL = "P|";
    private static final String MARCADOR_INPUT = "INPUT|";
    private static final String MARCADOR_FIM = "GESTAO_FIM";
    private static final String MARCADOR_ERRO = "GESTAO_ERRO";

    private GestaoRemotaSession() {
    }

    public static void executarNoServidor(BufferedReader entradaRede, PrintWriter saidaRede) {
        PrintStream saidaOriginal = System.out;
        InputStream entradaOriginal = System.in;
        PrintStream saidaRedeStream = criarPrintStreamRede(saidaRede);

        try {
            System.setOut(saidaRedeStream);
            MenuConsoleSimplificado.definirEntrada(new LinhaSocketInputStream(entradaRede));
            JPAUtil.configurarHostBanco("localhost");
            MenuConsoleSimplificado.exibirMenu();
        } catch (Exception e) {
            saidaRede.println(MARCADOR_ERRO + ";" + e.getMessage());
            saidaRede.flush();
        } finally {
            saidaRedeStream.flush();
            System.setOut(saidaOriginal);
            System.setIn(entradaOriginal);
            MenuConsoleSimplificado.restaurarEntradaPadrao();
        }
    }

    public static void executarNoCliente(PrintWriter saidaRede, BufferedReader entradaRede, Scanner console)
            throws IOException, InterruptedException {
        saidaRede.println("CMD:GESTAO_INICIAR");
        saidaRede.flush();

        String resposta = entradaRede.readLine();
        if (resposta == null) {
            System.out.println("Erro: servidor encerrou a conexao ao abrir a gestao.");
            return;
        }
        if (resposta.startsWith(MARCADOR_ERRO)) {
            System.out.println("Erro ao abrir gestao corporativa: " + resposta.substring(resposta.indexOf(';') + 1));
            return;
        }
        if (!"GESTAO_OK".equals(resposta)) {
            System.out.println("Resposta inesperada do servidor: " + resposta);
            return;
        }

        System.out.println("\nPainel remoto conectado ao servidor central.");
        System.out.println("(O banco de dados e acessado no servidor — nao e necessario PostgreSQL neste PC.)\n");

        AtomicBoolean sessaoAtiva = new AtomicBoolean(true);
        BlockingQueue<Object> sinaisEntrada = new LinkedBlockingQueue<>();

        Thread leitorRede = new Thread(
                () -> lerSaidaServidor(entradaRede, sessaoAtiva, sinaisEntrada),
                "gestao-remota-leitor"
        );
        leitorRede.setDaemon(true);
        leitorRede.start();

        while (sessaoAtiva.get()) {
            Object sinal = sinaisEntrada.poll(200, TimeUnit.MILLISECONDS);
            if (!sessaoAtiva.get()) {
                break;
            }
            if (sinal == null) {
                continue;
            }

            String linha = console.nextLine();
            saidaRede.println(linha);
            saidaRede.flush();
        }

        leitorRede.join(3_000);
    }

    private static void lerSaidaServidor(
            BufferedReader entradaRede,
            AtomicBoolean sessaoAtiva,
            BlockingQueue<Object> sinaisEntrada
    ) {
        try {
            String linha;
            while ((linha = entradaRede.readLine()) != null) {
                if (MARCADOR_FIM.equals(linha)) {
                    sessaoAtiva.set(false);
                    sinaisEntrada.offer(Boolean.FALSE);
                    break;
                }
                if (linha.startsWith(MARCADOR_ERRO)) {
                    System.out.println("Erro no servidor: " + linha.substring(linha.indexOf(';') + 1));
                    sessaoAtiva.set(false);
                    sinaisEntrada.offer(Boolean.FALSE);
                    break;
                }
                if (linha.startsWith(MARCADOR_INPUT)) {
                    sinaisEntrada.offer(Boolean.TRUE);
                    continue;
                }
                if (linha.startsWith(MARCADOR_PARCIAL)) {
                    System.out.print(linha.substring(MARCADOR_PARCIAL.length()));
                } else if (linha.startsWith(MARCADOR_LINHA)) {
                    System.out.println(linha.substring(MARCADOR_LINHA.length()));
                } else {
                    System.out.println(linha);
                }
            }
        } catch (IOException e) {
            System.out.println("Conexao com o servidor interrompida durante a gestao remota.");
        } finally {
            sessaoAtiva.set(false);
            sinaisEntrada.offer(Boolean.FALSE);
        }
    }

    private static PrintStream criarPrintStreamRede(PrintWriter saidaRede) {
        OutputStream base = new OutputStream() {
            private final StringBuilder acumulado = new StringBuilder();

            @Override
            public synchronized void write(byte[] bytes, int offset, int length) {
                if (length <= 0) {
                    return;
                }
                processar(new String(bytes, offset, length, StandardCharsets.UTF_8));
            }

            @Override
            public synchronized void write(int b) {
                write(new byte[]{(byte) b}, 0, 1);
            }

            @Override
            public synchronized void flush() {
                enviarParcial();
                saidaRede.flush();
            }

            private void processar(String chunk) {
                for (int i = 0; i < chunk.length(); i++) {
                    char c = chunk.charAt(i);
                    if (c == '\n') {
                        enviarLinhaCompleta();
                    } else if (c != '\r') {
                        acumulado.append(c);
                    }
                }
            }

            private void enviarParcial() {
                if (acumulado.isEmpty()) {
                    return;
                }
                saidaRede.println(MARCADOR_PARCIAL + acumulado);
                saidaRede.println(MARCADOR_INPUT);
                acumulado.setLength(0);
            }

            private void enviarLinhaCompleta() {
                saidaRede.println(MARCADOR_LINHA + acumulado);
                acumulado.setLength(0);
            }
        };

        return new PrintStream(base, true, StandardCharsets.UTF_8) {
            @Override
            public void print(String s) {
                super.print(s);
                super.flush();
            }

            @Override
            public void print(char c) {
                super.print(c);
                super.flush();
            }

            @Override
            public void print(int i) {
                super.print(i);
                super.flush();
            }

            @Override
            public void print(long l) {
                super.print(l);
                super.flush();
            }
        };
    }

    /**
     * Adapta linhas lidas do socket para um InputStream consumido pelo Scanner do menu.
     */
    private static final class LinhaSocketInputStream extends InputStream {

        private final BufferedReader reader;
        private byte[] buffer = new byte[0];
        private int posicao = 0;

        private LinhaSocketInputStream(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public int read() throws IOException {
            if (posicao >= buffer.length) {
                String linha = reader.readLine();
                if (linha == null) {
                    return -1;
                }
                buffer = (linha + "\n").getBytes(StandardCharsets.UTF_8);
                posicao = 0;
            }
            return buffer[posicao++] & 0xFF;
        }
    }
}
