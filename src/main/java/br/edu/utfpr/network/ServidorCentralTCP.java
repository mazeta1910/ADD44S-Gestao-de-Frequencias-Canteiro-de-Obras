package br.edu.utfpr.network;

import br.edu.utfpr.model.RegistroPonto;
import br.edu.utfpr.model.Trabalhador;
import br.edu.utfpr.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ServidorCentralTCP {

    public static void iniciarServidor() {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("SERVIDOR CENTRAL ATIVO - Escutando na porta 8080...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> processarRequisicao(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   SERVIDOR CENTRAL INICIADO (NODO 1)    ");
        System.out.println("   Aguardando conexoes dos canteiros...  ");
        System.out.println("=========================================");
        iniciarServidor();
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

                        long minutosTrabalhados = 0;
                        if (registroDiario.getHoraEntrada() != null && registroDiario.getHoraSaidaIntervalo() != null) {
                            minutosTrabalhados += Duration.between(registroDiario.getHoraEntrada(), registroDiario.getHoraSaidaIntervalo()).toMinutes();
                        }
                        if (registroDiario.getHoraRetornoIntervalo() != null) {
                            minutosTrabalhados += Duration.between(registroDiario.getHoraRetornoIntervalo(), agora).toMinutes();
                        } else if (registroDiario.getHoraEntrada() != null && registroDiario.getHoraSaidaIntervalo() == null) {
                            minutosTrabalhados += Duration.between(registroDiario.getHoraEntrada(), agora).toMinutes();
                        }

                        long metaMinutos = 480;
                        long saldo = minutosTrabalhados - metaMinutos;

                        String aviso = "";
                        if (saldo < 0) {
                            aviso = " [AVISO: Jornada incompleta. Saldo negativo de " + Math.abs(saldo) + " minutos]";
                        } else if (saldo > 0) {
                            aviso = " [Saldo positivo de " + saldo + " minutos]";
                        }

                        out.println("RESULTADO;Saida registrada as " + agora.format(fmt) + "." + aviso);
                    } else if (comando.equals("GET_ESTADO_JORNADA")) {
                        if (registroDiario.getHoraEntrada() == null) {
                            out.println("ESTADO;AGUARDANDO_ENTRADA");
                        } else if (registroDiario.getHoraSaidaIntervalo() == null && registroDiario.getHoraSaida() == null) {
                            out.println("ESTADO;EM_TRABALHO");
                        } else if (registroDiario.getHoraSaidaIntervalo() != null && registroDiario.getHoraRetornoIntervalo() == null) {
                            out.println("ESTADO;EM_INTERVALO");
                        } else if (registroDiario.getHoraRetornoIntervalo() != null && registroDiario.getHoraSaida() == null) {
                            out.println("ESTADO;EM_TRABALHO_POS_INTERVALO");
                        } else {
                            out.println("ESTADO;JORNADA_FINALIZADA");
                        }
                    } else if (comando.equals("FICHA_FREQUENCIA")) {
                        List<RegistroPonto> ficha = em.createQuery("SELECT r FROM RegistroPonto r WHERE r.trabalhador = :t ORDER BY r.dataRegistro DESC", RegistroPonto.class)
                                .setParameter("t", t).getResultList();

                        out.println("+------------+---------+---------+---------+---------+");
                        out.println("| DATA       | ENTRADA | INT.SAI | INT.RET | SAIDA   |");
                        out.println("+------------+---------+---------+---------+---------+");

                        for (RegistroPonto r : ficha) {
                            String data = r.getDataRegistro().toString();
                            String ent = r.getHoraEntrada() != null ? r.getHoraEntrada().format(fmt) : "--:--";
                            String is = r.getHoraSaidaIntervalo() != null ? r.getHoraSaidaIntervalo().format(fmt) : "--:--";
                            String ir = r.getHoraRetornoIntervalo() != null ? r.getHoraRetornoIntervalo().format(fmt) : "--:--";
                            String sai = r.getHoraSaida() != null ? r.getHoraSaida().format(fmt) : "--:--";

                            out.printf("| %-10s | %-7s | %-7s | %-7s | %-7s |\n", data, ent, is, ir, sai);
                        }
                        out.println("+------------+---------+---------+---------+---------+");
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