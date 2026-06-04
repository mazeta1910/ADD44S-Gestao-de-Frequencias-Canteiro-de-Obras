package br.edu.utfpr.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class TerminalCanteiroClienteTCP {

    public static void main(String[] args) {
        String ipServidor = NetworkConfig.resolverIpServidor(args);
        int portaServidor = NetworkConfig.resolverPortaServidor(args);
        boolean servidorLocal = NetworkConfig.isServidorLocal(ipServidor);

        Scanner scanner = new Scanner(System.in);

        System.out.println("=======================================");
        System.out.println("   TERMINAL DE ACESSO - CANTEIRO");
        System.out.println("=======================================");
        System.out.println("Conectando ao servidor: " + ipServidor + ":" + portaServidor);
        if (!servidorLocal) {
            System.out.println("[i] Terminal remoto: o painel de gestao (opcao 7) so funciona no PC do servidor.");
        }

        System.out.print("\nDigite seu CPF: ");
        String cpf = scanner.nextLine();

        try (
                Socket socket = new Socket(ipServidor, portaServidor);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            // Usa o Socket para autenticar no Nodo 1
            out.println("AUTH:" + cpf);
            String respostaAuth = in.readLine();

            if (respostaAuth != null && respostaAuth.startsWith("AUTH_SUCCESS")) {
                String[] partes = respostaAuth.split(";");
                boolean isAdmin = partes[1].equals("ADMIN");
                String nome = partes[2];

                System.out.println("\nBem-vindo, " + nome);
                if (isAdmin) {
                    System.out.println("[!] Privilegios de Administrador reconhecidos pelo Servidor.");
                }

                while (true) {
                    out.println("CMD:GET_ESTADO_JORNADA");
                    String estado = in.readLine().split(";")[1];

                    System.out.println("\n--- REGISTRO DE PONTO ---");

                    if (estado.equals("AGUARDANDO_ENTRADA")) {
                        System.out.println("1. Entrada");
                    } else if (estado.equals("EM_TRABALHO")) {
                        System.out.println("2. Saida para Intervalo");
                        System.out.println("4. Saida (Fim do Expediente)");
                    } else if (estado.equals("EM_INTERVALO")) {
                        System.out.println("3. Retorno do Intervalo");
                    } else if (estado.equals("EM_TRABALHO_POS_INTERVALO")) {
                        System.out.println("4. Saida (Fim do Expediente)");
                    } else if (estado.equals("JORNADA_FINALIZADA")) {
                        System.out.println(" * Sua jornada de trabalho hoje ja foi encerrada.");
                    }

                    System.out.println("5. Ver Ficha de Frequencia");
                    System.out.println("6. Sair");

                    // Painel de gestao usa o banco local; so esta disponivel no PC do servidor.
                    if (isAdmin && servidorLocal) {
                        System.out.println("7. Abrir Painel de Gestao (Menu Completo)");
                    }

                    System.out.print("Opcao: ");
                    String opcao = scanner.nextLine();

                    boolean opcaoValida = false;
                    if (opcao.equals("1") && estado.equals("AGUARDANDO_ENTRADA")) {
                        out.println("CMD:PONTO_ENTRADA"); opcaoValida = true;
                    } else if (opcao.equals("2") && estado.equals("EM_TRABALHO")) {
                        out.println("CMD:PONTO_INTERVALO"); opcaoValida = true;
                    } else if (opcao.equals("3") && estado.equals("EM_INTERVALO")) {
                        out.println("CMD:PONTO_RETORNO"); opcaoValida = true;
                    } else if (opcao.equals("4") && (estado.equals("EM_TRABALHO") || estado.equals("EM_TRABALHO_POS_INTERVALO"))) {
                        out.println("CMD:PONTO_SAIDA"); opcaoValida = true;
                    } else if (opcao.equals("5")) {
                        out.println("CMD:FICHA_FREQUENCIA"); opcaoValida = true;
                        System.out.println("\n--- FICHA DE FREQUENCIA ---");
                        String linha;
                        while (!(linha = in.readLine()).equals("FIM_FICHA")) {
                            System.out.println(linha);
                        }
                    } else if (opcao.equals("6")) {
                        out.println("CMD:SAIR");
                        break;
                    } else if (opcao.equals("7") && isAdmin && servidorLocal) {
                        opcaoValida = true;
                        System.out.println("\nIniciando modulo de gestao corporativa...");
                        br.edu.utfpr.MenuConsoleSimplificado.exibirMenu();
                        System.out.println("\nRetornando ao terminal de ponto...");
                    }

                    if (opcaoValida && !opcao.equals("5") && !opcao.equals("7")) {
                        System.out.println("\n" + in.readLine().split(";")[1]);
                    } else if (!opcaoValida) {
                        System.out.println("\nOpcao invalida para o momento atual da sua jornada ou permissao negada.");
                    }
                }
            } else {
                System.out.println("\nErro: Acesso Negado ou CPF nao reconhecido no banco de dados.");
            }

        } catch (Exception e) {
            System.out.println("ERRO: Nao foi possivel conectar ao servidor em " + ipServidor + ":" + portaServidor);
            System.out.println("Verifique se o ServidorCentralTCP esta rodando e se o firewall libera a porta.");
            System.out.println("Uso: java ... TerminalCanteiroClienteTCP [IP_DO_SERVIDOR] [PORTA]");
        } finally {
            scanner.close();
        }
    }
}