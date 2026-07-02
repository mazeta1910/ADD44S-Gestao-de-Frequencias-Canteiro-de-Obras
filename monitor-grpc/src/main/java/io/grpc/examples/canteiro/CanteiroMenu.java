package io.grpc.examples.canteiro;

import io.grpc.StatusRuntimeException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

/**
 * Menus interativos do monitor corporativo de canteiros.
 */
final class CanteiroMenu {

  private final CanteiroServiceGrpc.CanteiroServiceBlockingStub stub;
  private final Scanner scanner;

  CanteiroMenu(CanteiroServiceGrpc.CanteiroServiceBlockingStub stub, Scanner scanner) {
    this.stub = stub;
    this.scanner = scanner;
  }

  void executar() {
    System.out.println();
    System.out.println("========================================");
    System.out.println(" MONITOR CORPORATIVO - CANTEIROS DE OBRAS");
    System.out.println("========================================");

    boolean continuar = true;
    while (continuar) {
      System.out.println();
      System.out.println("MENU PRINCIPAL");
      System.out.println("  1 - Canteiros");
      System.out.println("  2 - Funcionarios");
      System.out.println("  3 - Materiais");
      System.out.println("  4 - Financas");
      System.out.println("  5 - Compras");
      System.out.println("  0 - Sair");
      System.out.print("Opcao: ");

      int opcao = lerInteiro();
      switch (opcao) {
        case 1:
          menuCanteiros();
          break;
        case 2:
          menuFuncionarios();
          break;
        case 3:
          menuMateriais();
          break;
        case 4:
          menuFinancas();
          break;
        case 5:
          menuCompras();
          break;
        case 0:
          continuar = false;
          System.out.println("Encerrando monitor...");
          break;
        default:
          System.out.println("Opcao invalida.");
      }
    }
  }

  private void menuCanteiros() {
    boolean voltar = false;
    while (!voltar) {
      System.out.println();
      System.out.println("--- CANTEIROS ---");
      System.out.println("  1 - Listar canteiros cadastrados");
      System.out.println("  2 - Consultar status operacional");
      System.out.println("  0 - Voltar");
      System.out.print("Opcao: ");

      switch (lerInteiro()) {
        case 1:
          exibirListaCanteiros();
          break;
        case 2:
          int id = pedirCanteiroId();
          if (id > 0) {
            exibirStatus(id);
          }
          break;
        case 0:
          voltar = true;
          break;
        default:
          System.out.println("Opcao invalida.");
      }
    }
  }

  private void menuFuncionarios() {
    boolean voltar = false;
    while (!voltar) {
      System.out.println();
      System.out.println("--- FUNCIONARIOS ---");
      System.out.println("  1 - Engenheiros por canteiro");
      System.out.println("  2 - Listar engenheiros");
      System.out.println("  3 - Listar pedreiros");
      System.out.println("  4 - Listar serventes");
      System.out.println("  5 - Listar mestres de obra");
      System.out.println("  6 - Listar todos os funcionarios");
      System.out.println("  7 - Funcionarios de um canteiro");
      System.out.println("  0 - Voltar");
      System.out.print("Opcao: ");

      switch (lerInteiro()) {
        case 1:
          exibirEngenheirosPorCanteiro();
          break;
        case 2:
          exibirFuncionarios(CanteiroRepository.TIPO_ENGENHEIRO, 0);
          break;
        case 3:
          exibirFuncionarios(CanteiroRepository.TIPO_PEDREIRO, 0);
          break;
        case 4:
          exibirFuncionarios(CanteiroRepository.TIPO_SERVENTE, 0);
          break;
        case 5:
          exibirFuncionarios(CanteiroRepository.TIPO_MESTRE, 0);
          break;
        case 6:
          exibirFuncionarios(CanteiroRepository.TIPO_TODOS, 0);
          break;
        case 7:
          int id = pedirCanteiroId();
          if (id > 0) {
            exibirFuncionarios(CanteiroRepository.TIPO_TODOS, id);
          }
          break;
        case 0:
          voltar = true;
          break;
        default:
          System.out.println("Opcao invalida.");
      }
    }
  }

  private void menuMateriais() {
    boolean voltar = false;
    while (!voltar) {
      System.out.println();
      System.out.println("--- MATERIAIS ---");
      System.out.println("  1 - Estoque geral (todos os canteiros)");
      System.out.println("  2 - Estoque por canteiro");
      System.out.println("  3 - Alertas de estoque baixo/critico");
      System.out.println("  0 - Voltar");
      System.out.print("Opcao: ");

      switch (lerInteiro()) {
        case 1:
          exibirMateriais(0, false);
          break;
        case 2:
          int id = pedirCanteiroId();
          if (id > 0) {
            exibirMateriais(id, false);
          }
          break;
        case 3:
          exibirMateriais(0, true);
          break;
        case 0:
          voltar = true;
          break;
        default:
          System.out.println("Opcao invalida.");
      }
    }
  }

  private void menuFinancas() {
    boolean voltar = false;
    while (!voltar) {
      System.out.println();
      System.out.println("--- FINANCAS ---");
      System.out.println("  1 - Resumo financeiro geral");
      System.out.println("  2 - Financas por canteiro");
      System.out.println("  3 - Obras com alerta orcamentario");
      System.out.println("  0 - Voltar");
      System.out.print("Opcao: ");

      switch (lerInteiro()) {
        case 1:
          exibirFinancas(0, false);
          break;
        case 2:
          int id = pedirCanteiroId();
          if (id > 0) {
            exibirFinancas(id, false);
          }
          break;
        case 3:
          exibirFinancas(0, true);
          break;
        case 0:
          voltar = true;
          break;
        default:
          System.out.println("Opcao invalida.");
      }
    }
  }

  private void menuCompras() {
    boolean voltar = false;
    while (!voltar) {
      System.out.println();
      System.out.println("--- COMPRAS ---");
      System.out.println("  1 - Todas as compras");
      System.out.println("  2 - Compras por canteiro");
      System.out.println("  3 - Pendentes de aprovacao");
      System.out.println("  4 - Aprovadas aguardando entrega");
      System.out.println("  5 - Entregues");
      System.out.println("  0 - Voltar");
      System.out.print("Opcao: ");

      switch (lerInteiro()) {
        case 1:
          exibirCompras(0, CanteiroRepository.STATUS_TODOS);
          break;
        case 2:
          int id = pedirCanteiroId();
          if (id > 0) {
            exibirCompras(id, CanteiroRepository.STATUS_TODOS);
          }
          break;
        case 3:
          exibirCompras(0, CanteiroRepository.STATUS_PENDENTE);
          break;
        case 4:
          exibirCompras(0, CanteiroRepository.STATUS_APROVADO);
          break;
        case 5:
          exibirCompras(0, CanteiroRepository.STATUS_ENTREGUE);
          break;
        case 0:
          voltar = true;
          break;
        default:
          System.out.println("Opcao invalida.");
      }
    }
  }

  private void exibirFinancas(int canteiroId, boolean apenasAlerta) {
    try {
      ListarFinancasReply resposta = stub.listFinancas(ListarFinancasRequest.newBuilder()
          .setCanteiroId(canteiroId)
          .setApenasAlerta(apenasAlerta)
          .build());

      if (resposta.getRegistrosCount() == 0) {
        System.out.println("Nenhum registro financeiro encontrado.");
        return;
      }

      System.out.println();
      System.out.println("========================================");
      System.out.println(" GESTAO FINANCEIRA");
      System.out.println("========================================");
      for (FinancaInfo f : resposta.getRegistrosList()) {
        System.out.println();
        System.out.println("Canteiro [" + f.getCanteiroId() + "] " + f.getCanteiroNome());
        System.out.printf(Locale.US, "  Orcamento total:     R$ %,.2f%n", f.getOrcamentoTotal());
        System.out.printf(Locale.US, "  Gasto acumulado:    R$ %,.2f (%.1f%%)%n",
            f.getGastoAcumulado(), f.getPercentualUtilizado());
        System.out.printf(Locale.US, "  Gasto do mes:        R$ %,.2f%n", f.getGastoMes());
        System.out.printf(Locale.US, "  Folha de pagamento:  R$ %,.2f%n", f.getFolhaPagamentoMes());
        System.out.printf(Locale.US, "  Materiais (mes):     R$ %,.2f%n", f.getCustoMateriaisMes());
        System.out.printf(Locale.US, "  Saldo disponivel:    R$ %,.2f%n", f.getSaldoDisponivel());
        System.out.println("  Situacao: [" + f.getSituacao() + "] " + f.getMensagem());
      }
      if (canteiroId == 0) {
        System.out.println();
        System.out.println("----------------------------------------");
        System.out.printf(Locale.US, "TOTAL ORCAMENTO: R$ %,.2f%n", resposta.getTotalOrcamento());
        System.out.printf(Locale.US, "TOTAL GASTO:     R$ %,.2f%n", resposta.getTotalGasto());
        System.out.printf(Locale.US, "TOTAL SALDO:     R$ %,.2f%n", resposta.getTotalSaldo());
      }
      System.out.println("========================================");
    } catch (StatusRuntimeException e) {
      System.out.println("Erro: " + e.getStatus());
    }
  }

  private void exibirCompras(int canteiroId, String status) {
    try {
      ListarComprasReply resposta = stub.listCompras(ListarComprasRequest.newBuilder()
          .setCanteiroId(canteiroId)
          .setStatus(status)
          .build());

      if (resposta.getComprasCount() == 0) {
        System.out.println("Nenhuma compra encontrada.");
        return;
      }

      System.out.println();
      System.out.println("--- PEDIDOS DE COMPRA ---");
      for (CompraInfo c : resposta.getComprasList()) {
        System.out.println();
        System.out.printf(Locale.US, "[%d] %s%n", c.getId(), c.getDescricao());
        System.out.println("    Canteiro: " + c.getCanteiroId() + " - " + c.getCanteiroNome());
        System.out.println("    Fornecedor: " + c.getFornecedor());
        System.out.println("    Quantidade: " + c.getQuantidade());
        System.out.printf(Locale.US, "    Valor: R$ %,.2f%n", c.getValor());
        System.out.println("    Data: " + c.getDataPedido() + " | Status: " + c.getStatus());
        System.out.println("    Solicitante: " + c.getSolicitante());
      }
      System.out.println();
      System.out.printf(Locale.US, "Valor total listado: R$ %,.2f (%d pedido(s))%n",
          resposta.getValorTotalListado(), resposta.getComprasCount());
    } catch (StatusRuntimeException e) {
      System.out.println("Erro: " + e.getStatus());
    }
  }

  private void exibirListaCanteiros() {
    try {
      ListarCanteirosReply lista = stub.listCanteiros(ListarCanteirosRequest.getDefaultInstance());
      System.out.println();
      System.out.println("Canteiros cadastrados:");
      for (CanteiroResumo c : lista.getCanteirosList()) {
        System.out.println("  [" + c.getId() + "] " + c.getNome());
        System.out.println("      " + c.getLocalizacao());
      }
    } catch (StatusRuntimeException e) {
      System.out.println("Erro: " + e.getStatus());
    }
  }

  private void exibirStatus(int canteiroId) {
    try {
      StatusReply r = stub.getStatus(StatusRequest.newBuilder().setCanteiroId(canteiroId).build());
      if (!r.getFound()) {
        System.out.println("Canteiro nao encontrado.");
        return;
      }

      String statusAbertura = r.getAberto() ? "ABERTO" : "FECHADO";
      System.out.println();
      System.out.println("========================================");
      System.out.println(" STATUS DO CANTEIRO");
      System.out.println("========================================");
      System.out.println("ID: " + canteiroId);
      System.out.println("Nome: " + r.getNome());
      System.out.println("Local: " + r.getLocalizacao());
      System.out.println("Consulta: " + r.getDataConsulta() + " as " + r.getHorarioConsulta());
      System.out.println("----------------------------------------");
      System.out.println("OPERACAO: " + statusAbertura + " | " + formatarSituacao(r.getSituacao()));
      System.out.println("Expediente: " + r.getHorarioAbertura() + " as " + r.getHorarioFechamento());
      System.out.println(r.getMensagem());
      System.out.println("----------------------------------------");
      System.out.println("EQUIPE: Pedreiros " + r.getPedreirosNoLocal() + "/" + r.getPedreirosCadastrados()
          + " | Engenheiros " + r.getEngenheirosNoLocal() + "/" + r.getEngenheirosCadastrados());
      System.out.printf(Locale.US, "AMBIENTE: %.1f C | Umidade %.1f%%%n",
          r.getTemperaturaCelsius(), r.getUmidadePercent());
      System.out.printf(Locale.US, "OBRA: %.1f%% concluida%n", r.getPercentualConclusao());
      System.out.println("========================================");
    } catch (StatusRuntimeException e) {
      System.out.println("Erro: " + e.getStatus());
    }
  }

  private void exibirEngenheirosPorCanteiro() {
    try {
      ListarFuncionariosReply resposta = stub.listFuncionarios(ListarFuncionariosRequest.newBuilder()
          .setTipo(CanteiroRepository.TIPO_ENGENHEIRO)
          .setCanteiroId(0)
          .build());

      if (resposta.getFuncionariosCount() == 0) {
        System.out.println("Nenhum engenheiro cadastrado.");
        return;
      }

      Map<Integer, List<FuncionarioInfo>> porCanteiro = new LinkedHashMap<>();
      for (FuncionarioInfo f : resposta.getFuncionariosList()) {
        porCanteiro.computeIfAbsent(f.getCanteiroId(), k -> new ArrayList<>()).add(f);
      }

      System.out.println();
      System.out.println("========================================");
      System.out.println(" ENGENHEIROS POR CANTEIRO");
      System.out.println("========================================");
      for (Map.Entry<Integer, List<FuncionarioInfo>> entry : porCanteiro.entrySet()) {
        FuncionarioInfo primeiro = entry.getValue().get(0);
        System.out.println();
        System.out.println("Canteiro [" + entry.getKey() + "] " + primeiro.getCanteiroNome());
        for (FuncionarioInfo eng : entry.getValue()) {
          String presenca = eng.getPresenteNoLocal() ? "NO LOCAL" : "AUSENTE";
          System.out.println("  - " + eng.getNome() + " (" + eng.getFuncao() + ") -> " + presenca);
        }
      }
      System.out.println("========================================");
    } catch (StatusRuntimeException e) {
      System.out.println("Erro: " + e.getStatus());
    }
  }

  private void exibirFuncionarios(String tipo, int canteiroId) {
    try {
      ListarFuncionariosReply resposta = stub.listFuncionarios(ListarFuncionariosRequest.newBuilder()
          .setTipo(tipo)
          .setCanteiroId(canteiroId)
          .build());

      if (resposta.getFuncionariosCount() == 0) {
        System.out.println("Nenhum funcionario encontrado.");
        return;
      }

      System.out.println();
      System.out.println("--- " + tituloTipo(tipo) + (canteiroId > 0 ? " (canteiro " + canteiroId + ")" : "") + " ---");
      for (FuncionarioInfo f : resposta.getFuncionariosList()) {
        String presenca = f.getPresenteNoLocal() ? "presente" : "ausente";
        System.out.printf(Locale.US, "  [%d] %-28s %-22s | Canteiro %d - %s (%s)%n",
            f.getId(), f.getNome(), f.getFuncao(), f.getCanteiroId(), f.getCanteiroNome(), presenca);
      }
      System.out.println("Total: " + resposta.getFuncionariosCount());
    } catch (StatusRuntimeException e) {
      System.out.println("Erro: " + e.getStatus());
    }
  }

  private void exibirMateriais(int canteiroId, boolean apenasBaixo) {
    try {
      ListarMateriaisReply resposta = stub.listMateriais(ListarMateriaisRequest.newBuilder()
          .setCanteiroId(canteiroId)
          .setApenasBaixo(apenasBaixo)
          .build());

      if (resposta.getMateriaisCount() == 0) {
        System.out.println(apenasBaixo ? "Nenhum material com estoque baixo." : "Nenhum material encontrado.");
        return;
      }

      System.out.println();
      String titulo = apenasBaixo ? "ALERTAS DE ESTOQUE" : "ESTOQUE DE MATERIAIS";
      System.out.println("--- " + titulo + " ---");
      int canteiroAtual = -1;
      for (MaterialInfo m : resposta.getMateriaisList()) {
        if (m.getCanteiroId() != canteiroAtual) {
          canteiroAtual = m.getCanteiroId();
          System.out.println();
          System.out.println("Canteiro [" + canteiroAtual + "] " + m.getCanteiroNome());
        }
        System.out.printf(Locale.US, "  %-22s %8.1f %-8s (min: %.1f) [%s]%n",
            m.getNome(), m.getQuantidade(), m.getUnidade(), m.getQuantidadeMinima(), m.getSituacao());
      }
      System.out.println();
      System.out.println("Itens listados: " + resposta.getMateriaisCount());
    } catch (StatusRuntimeException e) {
      System.out.println("Erro: " + e.getStatus());
    }
  }

  private int pedirCanteiroId() {
    exibirListaCanteiros();
    System.out.print("ID do canteiro (0 = cancelar): ");
    int id = lerInteiro();
    if (id == 0) {
      System.out.println("Operacao cancelada.");
    }
    return id;
  }

  private int lerInteiro() {
    if (!scanner.hasNextInt()) {
      scanner.nextLine();
      return -1;
    }
    int valor = scanner.nextInt();
    scanner.nextLine();
    return valor;
  }

  private static String tituloTipo(String tipo) {
    switch (tipo) {
      case CanteiroRepository.TIPO_ENGENHEIRO:
        return "ENGENHEIROS";
      case CanteiroRepository.TIPO_PEDREIRO:
        return "PEDREIROS";
      case CanteiroRepository.TIPO_SERVENTE:
        return "SERVENTES";
      case CanteiroRepository.TIPO_MESTRE:
        return "MESTRES DE OBRA";
      default:
        return "FUNCIONARIOS";
    }
  }

  private static String formatarSituacao(String situacao) {
    switch (situacao) {
      case "OPERACAO_NORMAL":
        return "Operacao normal";
      case "INTERVALO_ALMOCO":
        return "Intervalo de almoco";
      case "ABERTURA":
        return "Abertura";
      case "ENCERRAMENTO":
        return "Encerramento";
      case "FECHADO":
        return "Fechado";
      default:
        return situacao;
    }
  }
}
