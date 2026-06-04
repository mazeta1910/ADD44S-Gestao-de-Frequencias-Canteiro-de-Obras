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

/**
 * Executa o menu de gestao corporativa no servidor e encaminha entrada/saida
 * pela conexao TCP ja autenticada — evita exigir PostgreSQL acessivel na rede.
 */
public final class GestaoRemotaSession {

    private GestaoRemotaSession() {
    }

    public static void executarNoServidor(BufferedReader entradaRede, PrintWriter saidaRede) {
        PrintStream saidaOriginal = System.out;
        InputStream entradaOriginal = System.in;

        try {
            System.setOut(criarPrintStreamRede(saidaRede));
            MenuConsoleSimplificado.definirEntrada(new LinhaSocketInputStream(entradaRede));
            JPAUtil.configurarHostBanco("localhost");
            MenuConsoleSimplificado.exibirMenu();
        } catch (Exception e) {
            saidaRede.println("GESTAO_ERRO;" + e.getMessage());
            saidaRede.flush();
        } finally {
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
        if (resposta.startsWith("GESTAO_ERRO")) {
            System.out.println("Erro ao abrir gestao corporativa: " + resposta.substring(resposta.indexOf(';') + 1));
            return;
        }
        if (!"GESTAO_OK".equals(resposta)) {
            System.out.println("Resposta inesperada do servidor: " + resposta);
            return;
        }

        System.out.println("\nPainel remoto conectado ao servidor central.");
        System.out.println("(O banco de dados e acessado no servidor — nao e necessario PostgreSQL neste PC.)\n");

        Thread leitorRede = new Thread(() -> lerSaidaServidor(entradaRede), "gestao-remota-leitor");
        leitorRede.setDaemon(true);
        leitorRede.start();

        try {
            while (leitorRede.isAlive()) {
                if (!console.hasNextLine()) {
                    Thread.sleep(50);
                    continue;
                }
                String linha = console.nextLine();
                saidaRede.println(linha);
                saidaRede.flush();
            }
        } finally {
            leitorRede.join(2_000);
        }
    }

    private static void lerSaidaServidor(BufferedReader entradaRede) {
        try {
            String linha;
            while ((linha = entradaRede.readLine()) != null) {
                if ("GESTAO_FIM".equals(linha)) {
                    break;
                }
                if (linha.startsWith("GESTAO_ERRO")) {
                    System.out.println("Erro no servidor: " + linha.substring(linha.indexOf(';') + 1));
                    break;
                }
                if (linha.startsWith("P|")) {
                    System.out.print(linha.substring(2));
                } else if (linha.startsWith("L|")) {
                    System.out.println(linha.substring(2));
                } else {
                    System.out.println(linha);
                }
            }
        } catch (IOException e) {
            System.out.println("Conexao com o servidor interrompida durante a gestao remota.");
        }
    }

    private static PrintStream criarPrintStreamRede(PrintWriter saidaRede) {
        OutputStream base = new OutputStream() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void write(int b) {
                if (b == '\n') {
                    enviarLinhaCompleta();
                } else if (b != '\r') {
                    buffer.append((char) b);
                }
            }

            @Override
            public void flush() {
                if (!buffer.isEmpty()) {
                    saidaRede.println("P|" + buffer);
                    buffer.setLength(0);
                }
                saidaRede.flush();
            }

            private void enviarLinhaCompleta() {
                saidaRede.println("L|" + buffer);
                buffer.setLength(0);
            }
        };

        PrintStream stream = new PrintStream(base, true, StandardCharsets.UTF_8);
        return new PrintStream(stream, true, StandardCharsets.UTF_8) {
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
