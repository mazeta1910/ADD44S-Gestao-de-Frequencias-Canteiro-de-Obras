package br.edu.utfpr.network;

import br.edu.utfpr.model.RegistroPonto;
import br.edu.utfpr.model.Trabalhador;
import br.edu.utfpr.util.JPAUtil;
import br.edu.utfpr.util.JornadaUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ServidorCentralTCP {

    public static void iniciarServidor(int porta) {
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("SERVIDOR CENTRAL ATIVO - Escutando na porta " + porta + "...");
            System.out.println("Terminais remotos podem conectar usando um destes IPs:");

            List<String> ipsLocais = NetworkConfig.listarIpsLocais();
            if (ipsLocais.isEmpty()) {
                System.out.println("  - " + NetworkConfig.IP_PADRAO + " (somente neste computador)");
            } else {
                for (String ip : ipsLocais) {
                    System.out.println("  - " + ip + ":" + porta);
                }
            }
            System.out.println("Exemplo no outro PC: java ... TerminalCanteiroClienteTCP " +
                    (ipsLocais.isEmpty() ? NetworkConfig.IP_PADRAO : ipsLocais.get(0)) + " " + porta);
            System.out.println("Gestao corporativa remota (opcao 7): executada no servidor via TCP.");
            System.out.println("Certifique-se de que o PostgreSQL esta rodando neste PC (localhost:5432).");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> processarRequisicao(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int porta = NetworkConfig.resolverPortaServidor(args);

        System.out.println("=========================================");
        System.out.println("   SERVIDOR CENTRAL INICIADO (NODO 1)    ");
        System.out.println("   Aguardando conexoes dos canteiros...  ");
        System.out.println("=========================================");
        iniciarServidor(porta);
    }

    private static void processarRequisicao(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String mensagem;
            String cpfAutenticado = null;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

            while ((mensagem = in.readLine()) != null) {
                if (mensagem.startsWith("AUTH:")) {
                    cpfAutenticado = mensagem.substring(5);
                    EntityManager em = JPAUtil.getEntityManager();
                    try {
                        Trabalhador t = em.createQuery("SELECT t FROM Trabalhador t WHERE t.cpf = :cpf", Trabalhador.class)
                                .setParameter("cpf", cpfAutenticado).getSingleResult();

                        if (t.isAdministrador()) {
                            out.println("AUTH_SUCCESS;ADMIN;" + t.getNomeCompleto());
                        } else {
                            out.println("AUTH_SUCCESS;OPERACIONAL;" + t.getNomeCompleto());
                        }
                    } catch (NoResultException e) {
                        out.println("AUTH_FAILED;CPF_INVALIDO");
                    }
                    em.close();
                } else if (mensagem.startsWith("CMD:") && cpfAutenticado != null) {
                    String comando = mensagem.substring(4);

                    if (comando.equals("GESTAO_INICIAR")) {
                        EntityManager emGestao = JPAUtil.getEntityManager();
                        try {
                            Trabalhador admin = emGestao.createQuery(
                                            "SELECT t FROM Trabalhador t WHERE t.cpf = :cpf", Trabalhador.class)
                                    .setParameter("cpf", cpfAutenticado)
                                    .getSingleResult();

                            if (!admin.isAdministrador()) {
                                out.println("GESTAO_ERRO;PERMISSAO_NEGADA");
                            } else {
                                out.println("GESTAO_OK");
                                out.flush();
                                GestaoRemotaSession.executarNoServidor(in, out);
                                out.println("GESTAO_FIM");
                                out.flush();
                            }
                        } catch (NoResultException e) {
                            out.println("GESTAO_ERRO;CPF_INVALIDO");
                        } finally {
                            emGestao.close();
                        }
                        continue;
                    }

                    EntityManager em = JPAUtil.getEntityManager();
                    em.getTransaction().begin();

                    Trabalhador t = em.createQuery("SELECT t FROM Trabalhador t WHERE t.cpf = :cpf", Trabalhador.class)
                            .setParameter("cpf", cpfAutenticado).getSingleResult();

                    RegistroPonto registroDiario;
                    try {
                        registroDiario = em.createQuery("SELECT r FROM RegistroPonto r WHERE r.trabalhador = :t AND r.dataRegistro = :data", RegistroPonto.class)
                                .setParameter("t", t)
                                .setParameter("data", LocalDate.now())
                                .getSingleResult();
                    } catch (NoResultException e) {
                        registroDiario = new RegistroPonto();
                        registroDiario.setTrabalhador(t);
                        registroDiario.setDataRegistro(LocalDate.now());
                        em.persist(registroDiario);
                    }

                    LocalTime agora = LocalTime.now();

                    if (comando.equals("PONTO_ENTRADA")) {
                        registroDiario.setHoraEntrada(agora);
                        out.println("RESULTADO;Entrada registrada as " + agora.format(fmt));
                    } else if (comando.equals("PONTO_INTERVALO")) {
                        registroDiario.setHoraSaidaIntervalo(agora);
                        out.println("RESULTADO;Saida para intervalo as " + agora.format(fmt));
                    } else if (comando.equals("PONTO_RETORNO")) {
                        registroDiario.setHoraRetornoIntervalo(agora);
                        out.println("RESULTADO;Retorno do intervalo as " + agora.format(fmt));
                    } else if (comando.equals("PONTO_SAIDA")) {
                        registroDiario.setHoraSaida(agora);

                        long saldo = JornadaUtil.calcularSaldo(registroDiario, t, agora);
                        String aviso = "";
                        if (saldo < 0) {
                            aviso = " [AVISO: Jornada incompleta. " + JornadaUtil.formatarSaldo(saldo) + "]";
                        } else if (saldo > 0) {
                            aviso = " [" + JornadaUtil.formatarSaldo(saldo) + "]";
                        }

                        out.println("RESULTADO;Saida registrada as " + agora.format(fmt) + "." + aviso);
                    } else if (comando.equals("GET_ESTADO_JORNADA")) {
                        String estado;
                        if (registroDiario.getHoraEntrada() == null) {
                            estado = "AGUARDANDO_ENTRADA";
                        } else if (registroDiario.getHoraSaidaIntervalo() == null && registroDiario.getHoraSaida() == null) {
                            estado = "EM_TRABALHO";
                        } else if (registroDiario.getHoraSaidaIntervalo() != null && registroDiario.getHoraRetornoIntervalo() == null) {
                            estado = "EM_INTERVALO";
                        } else if (registroDiario.getHoraRetornoIntervalo() != null && registroDiario.getHoraSaida() == null) {
                            estado = "EM_TRABALHO_POS_INTERVALO";
                        } else {
                            estado = "JORNADA_FINALIZADA";
                        }
                        out.println(JornadaUtil.montarRespostaEstado(estado, registroDiario, t, agora));
                    } else if (comando.equals("FICHA_FREQUENCIA")) {
                        List<RegistroPonto> ficha = em.createQuery("SELECT r FROM RegistroPonto r WHERE r.trabalhador = :t ORDER BY r.dataRegistro DESC", RegistroPonto.class)
                                .setParameter("t", t).getResultList();

                        out.println("+------------+---------+---------+---------+---------+---------+");
                        out.println("| DATA       | ENTRADA | INT.SAI | INT.RET | SAIDA   | SALDO   |");
                        out.println("+------------+---------+---------+---------+---------+---------+");

                        for (RegistroPonto r : ficha) {
                            String data = r.getDataRegistro().toString();
                            String ent = r.getHoraEntrada() != null ? r.getHoraEntrada().format(fmt) : "--:--";
                            String is = r.getHoraSaidaIntervalo() != null ? r.getHoraSaidaIntervalo().format(fmt) : "--:--";
                            String ir = r.getHoraRetornoIntervalo() != null ? r.getHoraRetornoIntervalo().format(fmt) : "--:--";
                            String sai = r.getHoraSaida() != null ? r.getHoraSaida().format(fmt) : "--:--";
                            String saldo = "--:--";
                            if (r.getHoraSaida() != null) {
                                long saldoMinutos = JornadaUtil.calcularSaldo(r, t, r.getHoraSaida());
                                saldo = JornadaUtil.formatarSaldoCurto(saldoMinutos);
                            }

                            out.printf("| %-10s | %-7s | %-7s | %-7s | %-7s | %-7s |\n", data, ent, is, ir, sai, saldo);
                        }
                        out.println("+------------+---------+---------+---------+---------+---------+");
                        out.println("FIM_FICHA");
                    } else if (comando.equals("SAIR")) {
                        out.println("DESCONECTADO");
                        em.getTransaction().commit();
                        em.close();
                        break;
                    }

                    if (em.getTransaction().isActive()) {
                        em.getTransaction().commit();
                    }
                    em.close();
                }
            }
        } catch (Exception e) {
            System.out.println("Conexao encerrada.");
        }
    }
}