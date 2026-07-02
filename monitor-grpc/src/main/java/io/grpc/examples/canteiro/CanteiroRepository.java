package io.grpc.examples.canteiro;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Catalogo em memoria dos canteiros, funcionarios e materiais.
 */
final class CanteiroRepository {

  static final String TIPO_TODOS = "TODOS";
  static final String TIPO_ENGENHEIRO = "ENGENHEIRO";
  static final String TIPO_PEDREIRO = "PEDREIRO";
  static final String TIPO_SERVENTE = "SERVENTE";
  static final String TIPO_MESTRE = "MESTRE_DE_OBRAS";
  static final String STATUS_TODOS = "TODOS";
  static final String STATUS_PENDENTE = "PENDENTE";
  static final String STATUS_APROVADO = "APROVADO";
  static final String STATUS_ENTREGUE = "ENTREGUE";
  static final String STATUS_CANCELADO = "CANCELADO";

  private static final LocalTime HORA_ABERTURA = LocalTime.of(7, 0);
  private static final LocalTime HORA_FECHAMENTO = LocalTime.of(18, 0);
  private static final LocalTime INICIO_ALMOCO = LocalTime.of(12, 0);
  private static final LocalTime FIM_ALMOCO = LocalTime.of(13, 0);
  private static final LocalTime ENTRADA_ENGENHEIROS = LocalTime.of(8, 0);
  private static final LocalTime SAIDA_ENGENHEIROS = LocalTime.of(17, 0);

  private final Map<Integer, CanteiroBase> canteiros = criarCanteiros();
  private final List<FuncionarioBase> funcionarios = criarFuncionarios();
  private final List<MaterialBase> materiais = criarMateriais();
  private final List<FinancaBase> financas = criarFinancas();
  private final List<CompraBase> compras = criarCompras();

  ListarCanteirosReply listarCanteiros() {
    ListarCanteirosReply.Builder builder = ListarCanteirosReply.newBuilder();
    canteiros.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(e -> builder.addCanteiros(CanteiroResumo.newBuilder()
            .setId(e.getKey())
            .setNome(e.getValue().nome)
            .setLocalizacao(e.getValue().localizacao)
            .build()));
    return builder.build();
  }

  StatusReply montarStatus(int canteiroId, LocalDateTime agora) {
    CanteiroBase base = canteiros.get(canteiroId);
    if (base == null) {
      return StatusReply.newBuilder().setFound(false).setNome("Canteiro nao encontrado").build();
    }

    LocalTime hora = agora.toLocalTime();
    boolean aberto = estaAberto(hora);
    String situacao = resolverSituacao(hora, aberto);

    return StatusReply.newBuilder()
        .setFound(true)
        .setNome(base.nome)
        .setLocalizacao(base.localizacao)
        .setDataConsulta(agora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        .setHorarioConsulta(agora.format(DateTimeFormatter.ofPattern("HH:mm:ss")))
        .setAberto(aberto)
        .setHorarioAbertura("07:00")
        .setHorarioFechamento("18:00")
        .setSituacao(situacao)
        .setPedreirosCadastrados(contarPorTipo(canteiroId, TIPO_PEDREIRO))
        .setPedreirosNoLocal(calcularPresentes(canteiroId, TIPO_PEDREIRO, hora, aberto, situacao))
        .setPedreirosEmIntervalo(calcularEmIntervalo(canteiroId, situacao))
        .setEngenheirosCadastrados(contarPorTipo(canteiroId, TIPO_ENGENHEIRO))
        .setEngenheirosNoLocal(calcularPresentes(canteiroId, TIPO_ENGENHEIRO, hora, aberto, situacao))
        .setTemperaturaCelsius(calcularTemperatura(base.tempBase, hora))
        .setUmidadePercent(calcularUmidade(hora))
        .setPercentualConclusao(calcularConclusao(base.conclusaoBase, agora))
        .setMensagem(montarMensagem(aberto, situacao, canteiroId, hora))
        .build();
  }

  ListarFuncionariosReply listarFuncionarios(String tipo, int canteiroId, LocalDateTime agora) {
    LocalTime hora = agora.toLocalTime();
    boolean aberto = estaAberto(hora);
    String situacao = resolverSituacao(hora, aberto);
    String tipoFiltro = tipo == null || tipo.isEmpty() ? TIPO_TODOS : tipo.toUpperCase(Locale.ROOT);

    ListarFuncionariosReply.Builder builder = ListarFuncionariosReply.newBuilder();
    for (FuncionarioBase f : funcionarios) {
      if (!TIPO_TODOS.equals(tipoFiltro) && !f.tipo.equals(tipoFiltro)) {
        continue;
      }
      if (canteiroId > 0 && f.canteiroId != canteiroId) {
        continue;
      }
      CanteiroBase c = canteiros.get(f.canteiroId);
      builder.addFuncionarios(FuncionarioInfo.newBuilder()
          .setId(f.id)
          .setNome(f.nome)
          .setTipo(f.tipo)
          .setFuncao(f.funcao)
          .setCanteiroId(f.canteiroId)
          .setCanteiroNome(c != null ? c.nome : "-")
          .setPresenteNoLocal(calcularPresente(f, hora, aberto, situacao))
          .build());
    }
    return builder.build();
  }

  ListarMateriaisReply listarMateriais(int canteiroId, boolean apenasBaixo) {
    ListarMateriaisReply.Builder builder = ListarMateriaisReply.newBuilder();
    for (MaterialBase m : materiais) {
      if (canteiroId > 0 && m.canteiroId != canteiroId) {
        continue;
      }
      String situacao = resolverSituacaoEstoque(m.quantidade, m.quantidadeMinima);
      if (apenasBaixo && "OK".equals(situacao)) {
        continue;
      }
      CanteiroBase c = canteiros.get(m.canteiroId);
      builder.addMateriais(MaterialInfo.newBuilder()
          .setCanteiroId(m.canteiroId)
          .setCanteiroNome(c != null ? c.nome : "-")
          .setNome(m.nome)
          .setUnidade(m.unidade)
          .setQuantidade(m.quantidade)
          .setQuantidadeMinima(m.quantidadeMinima)
          .setSituacao(situacao)
          .build());
    }
    return builder.build();
  }

  ListarFinancasReply listarFinancas(int canteiroId, boolean apenasAlerta) {
    ListarFinancasReply.Builder builder = ListarFinancasReply.newBuilder();
    double totalOrc = 0;
    double totalGasto = 0;
    double totalSaldo = 0;

    for (FinancaBase f : financas) {
      if (canteiroId > 0 && f.canteiroId != canteiroId) {
        continue;
      }
      String situacao = resolverSituacaoFinanceira(f);
      if (apenasAlerta && "OK".equals(situacao)) {
        continue;
      }
      CanteiroBase c = canteiros.get(f.canteiroId);
      double saldo = f.orcamentoTotal - f.gastoAcumulado;
      double pct = (f.gastoAcumulado / f.orcamentoTotal) * 100.0;

      builder.addRegistros(FinancaInfo.newBuilder()
          .setCanteiroId(f.canteiroId)
          .setCanteiroNome(c != null ? c.nome : "-")
          .setOrcamentoTotal(f.orcamentoTotal)
          .setGastoAcumulado(f.gastoAcumulado)
          .setGastoMes(f.gastoMes)
          .setSaldoDisponivel(saldo)
          .setPercentualUtilizado(Math.round(pct * 10.0) / 10.0)
          .setFolhaPagamentoMes(f.folhaMes)
          .setCustoMateriaisMes(f.materiaisMes)
          .setSituacao(situacao)
          .setMensagem(montarMensagemFinanceira(situacao, pct, saldo))
          .build());

      totalOrc += f.orcamentoTotal;
      totalGasto += f.gastoAcumulado;
      totalSaldo += saldo;
    }

    return builder
        .setTotalOrcamento(totalOrc)
        .setTotalGasto(totalGasto)
        .setTotalSaldo(totalSaldo)
        .build();
  }

  ListarComprasReply listarCompras(int canteiroId, String status) {
    String statusFiltro = status == null || status.isEmpty()
        ? STATUS_TODOS : status.toUpperCase(Locale.ROOT);

    ListarComprasReply.Builder builder = ListarComprasReply.newBuilder();
    double valorTotal = 0;

    for (CompraBase c : compras) {
      if (canteiroId > 0 && c.canteiroId != canteiroId) {
        continue;
      }
      if (!STATUS_TODOS.equals(statusFiltro) && !c.status.equals(statusFiltro)) {
        continue;
      }
      CanteiroBase cant = canteiros.get(c.canteiroId);
      builder.addCompras(CompraInfo.newBuilder()
          .setId(c.id)
          .setCanteiroId(c.canteiroId)
          .setCanteiroNome(cant != null ? cant.nome : "-")
          .setDescricao(c.descricao)
          .setFornecedor(c.fornecedor)
          .setQuantidade(c.quantidade)
          .setValor(c.valor)
          .setDataPedido(c.dataPedido)
          .setStatus(c.status)
          .setSolicitante(c.solicitante)
          .build());
      valorTotal += c.valor;
    }

    return builder.setValorTotalListado(valorTotal).build();
  }

  String nomeCanteiro(int id) {
    CanteiroBase c = canteiros.get(id);
    return c != null ? c.nome : "Desconhecido";
  }

  private int contarPorTipo(int canteiroId, String tipo) {
    return (int) funcionarios.stream()
        .filter(f -> f.canteiroId == canteiroId && f.tipo.equals(tipo))
        .count();
  }

  private int calcularPresentes(int canteiroId, String tipo, LocalTime hora, boolean aberto, String situacao) {
    return (int) funcionarios.stream()
        .filter(f -> f.canteiroId == canteiroId && f.tipo.equals(tipo))
        .filter(f -> calcularPresente(f, hora, aberto, situacao))
        .count();
  }

  private int calcularEmIntervalo(int canteiroId, String situacao) {
    if (!"INTERVALO_ALMOCO".equals(situacao)) {
      return 0;
    }
    int pedreiros = contarPorTipo(canteiroId, TIPO_PEDREIRO);
    return Math.max(0, (int) Math.round(pedreiros * 0.50));
  }

  private boolean calcularPresente(FuncionarioBase f, LocalTime hora, boolean aberto, String situacao) {
    if (!aberto) {
      return false;
    }
    if (TIPO_ENGENHEIRO.equals(f.tipo) || TIPO_MESTRE.equals(f.tipo)) {
      return !hora.isBefore(ENTRADA_ENGENHEIROS) && hora.isBefore(SAIDA_ENGENHEIROS);
    }
    if ("INTERVALO_ALMOCO".equals(situacao) && (TIPO_PEDREIRO.equals(f.tipo) || TIPO_SERVENTE.equals(f.tipo))) {
      return f.id % 3 != 0;
    }
    return f.id % 5 != 0;
  }

  private String montarMensagem(boolean aberto, String situacao, int canteiroId, LocalTime hora) {
    if (!aberto) {
      return "Canteiro fora do horario de operacao. Nenhuma equipe no local.";
    }
    if ("INTERVALO_ALMOCO".equals(situacao)) {
      return "Horario de almoco. Parte da equipe retornara em breve.";
    }
    int pedreiros = calcularPresentes(canteiroId, TIPO_PEDREIRO, hora, aberto, situacao);
    int total = contarPorTipo(canteiroId, TIPO_PEDREIRO);
    return "Operacao em andamento com " + pedreiros + " de " + total + " pedreiros no local.";
  }

  private static boolean estaAberto(LocalTime hora) {
    return !hora.isBefore(HORA_ABERTURA) && hora.isBefore(HORA_FECHAMENTO);
  }

  private static String resolverSituacao(LocalTime hora, boolean aberto) {
    if (!aberto) {
      return "FECHADO";
    }
    if (!hora.isBefore(INICIO_ALMOCO) && hora.isBefore(FIM_ALMOCO)) {
      return "INTERVALO_ALMOCO";
    }
    if (hora.isBefore(LocalTime.of(8, 0))) {
      return "ABERTURA";
    }
    if (!hora.isBefore(LocalTime.of(17, 0))) {
      return "ENCERRAMENTO";
    }
    return "OPERACAO_NORMAL";
  }

  private static String resolverSituacaoEstoque(double qtd, double min) {
    if (qtd <= min * 0.5) {
      return "CRITICO";
    }
    if (qtd <= min) {
      return "BAIXO";
    }
    return "OK";
  }

  private static String resolverSituacaoFinanceira(FinancaBase f) {
    double pct = (f.gastoAcumulado / f.orcamentoTotal) * 100.0;
    if (pct >= 95.0) {
      return "CRITICO";
    }
    if (pct >= 80.0) {
      return "ATENCAO";
    }
    return "OK";
  }

  private static String montarMensagemFinanceira(String situacao, double pct, double saldo) {
    if ("CRITICO".equals(situacao)) {
      return "Orcamento quase esgotado. Revisar custos com urgencia.";
    }
    if ("ATENCAO".equals(situacao)) {
      return "Gastos elevados. Saldo disponivel: R$ " + String.format(Locale.US, "%.2f", saldo);
    }
    return "Obra dentro do orcamento (" + String.format(Locale.US, "%.1f", pct) + "% utilizado).";
  }

  private static double calcularTemperatura(double tempBase, LocalTime hora) {
    double h = hora.getHour() + hora.getMinute() / 60.0;
    return Math.round((tempBase + 4.0 * Math.sin((h - 6.0) * Math.PI / 12.0)) * 10.0) / 10.0;
  }

  private static double calcularUmidade(LocalTime hora) {
    double h = hora.getHour() + hora.getMinute() / 60.0;
    return Math.round((65.0 - 15.0 * Math.sin((h - 6.0) * Math.PI / 12.0)) * 10.0) / 10.0;
  }

  private static double calcularConclusao(double base, LocalDateTime agora) {
    double acrescimo = (agora.getDayOfYear() % 30) * 0.05;
    return Math.min(99.9, Math.round((base + acrescimo) * 10.0) / 10.0);
  }

  private static Map<Integer, CanteiroBase> criarCanteiros() {
    Map<Integer, CanteiroBase> dados = new HashMap<>();
    dados.put(1, new CanteiroBase("Obra Residencial Vila Nova", "Rua das Flores, 123 - Curitiba/PR", 24.0, 67.0));
    dados.put(2, new CanteiroBase("Construcao Comercial Centro", "Av. Principal, 456 - Curitiba/PR", 26.5, 42.5));
    dados.put(3, new CanteiroBase("Ponte Rodoviaria BR-277", "Km 162 - Pato Branco/PR", 25.0, 28.0));
    dados.put(4, new CanteiroBase("Condominio Residencial Horizonte", "Rua Parana, 890 - Pato Branco/PR", 23.5, 55.0));
    dados.put(5, new CanteiroBase("Reforma Escola Municipal", "Bairro Sao Francisco - Pato Branco/PR", 24.0, 81.5));
    return dados;
  }

  private static List<FuncionarioBase> criarFuncionarios() {
    List<FuncionarioBase> lista = new ArrayList<>();
    int id = 1;

    lista.addAll(Arrays.asList(
        new FuncionarioBase(id++, "Carlos Alberto Mendes", TIPO_ENGENHEIRO, "Engenheiro Civil", 1),
        new FuncionarioBase(id++, "Ana Paula Costa", TIPO_ENGENHEIRO, "Engenheira de Seguranca", 1),
        new FuncionarioBase(id++, "Roberto Lima", TIPO_ENGENHEIRO, "Engenheiro Civil", 2),
        new FuncionarioBase(id++, "Fernanda Rocha", TIPO_ENGENHEIRO, "Engenheira Estrutural", 3),
        new FuncionarioBase(id++, "Paulo Henrique Dias", TIPO_ENGENHEIRO, "Engenheiro Civil", 3),
        new FuncionarioBase(id++, "Mariana Souza", TIPO_ENGENHEIRO, "Engenheira Ambiental", 3),
        new FuncionarioBase(id++, "Lucas Ferreira", TIPO_ENGENHEIRO, "Engenheiro Civil", 4),
        new FuncionarioBase(id++, "Beatriz Almeida", TIPO_ENGENHEIRO, "Engenheira de Projetos", 4),
        new FuncionarioBase(id++, "Ricardo Gomes", TIPO_ENGENHEIRO, "Engenheiro Civil", 5)
    ));

    lista.addAll(Arrays.asList(
        new FuncionarioBase(id++, "Joao da Silva", TIPO_MESTRE, "Mestre de Obras", 1),
        new FuncionarioBase(id++, "Pedro Santos", TIPO_MESTRE, "Mestre de Obras", 2),
        new FuncionarioBase(id++, "Marcos Oliveira", TIPO_MESTRE, "Mestre de Obras", 3),
        new FuncionarioBase(id++, "Antonio Pereira", TIPO_MESTRE, "Mestre de Obras", 4),
        new FuncionarioBase(id++, "Eduardo Martins", TIPO_MESTRE, "Mestre de Obras", 5)
    ));

    String[] nomesPedreiros = {
        "Jose Alves", "Francisco Nunes", "Luiz Barbosa", "Raimundo Cruz",
        "Sebastiao Reis", "Valdir Campos", "Gilberto Ramos", "Claudio Pires"
    };
    int[] canteirosPedreiros = {1, 1, 1, 1, 2, 2, 3, 3, 3, 4, 4, 5};
    for (int i = 0; i < canteirosPedreiros.length; i++) {
      String nome = nomesPedreiros[i % nomesPedreiros.length] + " " + (i + 1);
      lista.add(new FuncionarioBase(id++, nome, TIPO_PEDREIRO, "Pedreiro", canteirosPedreiros[i]));
    }

    String[] nomesServentes = {"Carlos M.", "Diego F.", "Elias T.", "Fabio L.", "Gustavo H."};
    int[] canteirosServentes = {1, 1, 2, 3, 3, 4, 5};
    for (int i = 0; i < canteirosServentes.length; i++) {
      lista.add(new FuncionarioBase(id++, nomesServentes[i % nomesServentes.length] + " " + (i + 1),
          TIPO_SERVENTE, "Servente de Obras", canteirosServentes[i]));
    }

    return Collections.unmodifiableList(lista);
  }

  private static List<MaterialBase> criarMateriais() {
    List<MaterialBase> lista = new ArrayList<>();
    adicionarMateriaisCanteiro(lista, 1,
        mat(1, "Cimento CP-II", "sacas", 180, 80),
        mat(1, "Areia media", "m3", 42, 20),
        mat(1, "Brita 1", "m3", 35, 15),
        mat(1, "Ferro 10mm", "kg", 2200, 1000),
        mat(1, "Tijolo ceramico", "milheiro", 12, 8),
        mat(1, "Kit EPI basico", "kits", 25, 15));
    adicionarMateriaisCanteiro(lista, 2,
        mat(2, "Cimento CP-II", "sacas", 95, 60),
        mat(2, "Areia media", "m3", 18, 20),
        mat(2, "Brita 1", "m3", 22, 15),
        mat(2, "Ferro 12mm", "kg", 1500, 800),
        mat(2, "Bloco de concreto", "milheiro", 6, 10));
    adicionarMateriaisCanteiro(lista, 3,
        mat(3, "Cimento CP-IV", "sacas", 320, 120),
        mat(3, "Areia media", "m3", 80, 30),
        mat(3, "Brita 0", "m3", 55, 25),
        mat(3, "Ferro 16mm", "kg", 4800, 2000),
        mat(3, "Forma metalica", "unidades", 40, 20));
    adicionarMateriaisCanteiro(lista, 4,
        mat(4, "Cimento CP-II", "sacas", 140, 70),
        mat(4, "Areia fina", "m3", 28, 18),
        mat(4, "Tijolo ceramico", "milheiro", 9, 12),
        mat(4, "Argamassa AC-III", "sacas", 45, 50));
    adicionarMateriaisCanteiro(lista, 5,
        mat(5, "Cimento CP-II", "sacas", 35, 40),
        mat(5, "Massa corrida", "sacas", 22, 25),
        mat(5, "Tinta latex", "latas", 18, 20),
        mat(5, "Piso vinilico", "m2", 120, 80));
    return Collections.unmodifiableList(lista);
  }

  private static List<FinancaBase> criarFinancas() {
    return Arrays.asList(
        new FinancaBase(1, 2_800_000, 1_876_000, 142_000, 98_000, 44_000),
        new FinancaBase(2, 1_500_000, 638_000, 78_500, 52_000, 26_500),
        new FinancaBase(3, 4_200_000, 3_990_000, 210_000, 115_000, 95_000),
        new FinancaBase(4, 2_100_000, 1_155_000, 95_000, 68_000, 27_000),
        new FinancaBase(5, 680_000, 554_000, 48_000, 31_000, 17_000));
  }

  private static List<CompraBase> criarCompras() {
    List<CompraBase> lista = new ArrayList<>();
    int id = 1;
    lista.add(new CompraBase(id++, 1, "Cimento CP-II (200 sacas)", "Votorantim Cimentos",
        "200 sacas", 18_400, "28/06/2026", STATUS_ENTREGUE, "Carlos Alberto Mendes"));
    lista.add(new CompraBase(id++, 1, "Ferro 10mm", "Gerdau",
        "3.000 kg", 24_600, "30/06/2026", STATUS_APROVADO, "Ana Paula Costa"));
    lista.add(new CompraBase(id++, 2, "Bloco de concreto", "Concreteira Sul",
        "5 milheiro", 12_800, "01/07/2026", STATUS_PENDENTE, "Roberto Lima"));
    lista.add(new CompraBase(id++, 3, "Formas metalicas", "Metformas Obras",
        "20 unidades", 45_000, "25/06/2026", STATUS_ENTREGUE, "Fernanda Rocha"));
    lista.add(new CompraBase(id++, 3, "Brita 0", "Mineradora Planalto",
        "80 m3", 9_600, "29/06/2026", STATUS_APROVADO, "Paulo Henrique Dias"));
    lista.add(new CompraBase(id++, 3, "Locacao de guindaste", "LocObras PR",
        "15 dias", 38_500, "01/07/2026", STATUS_PENDENTE, "Mariana Souza"));
    lista.add(new CompraBase(id++, 4, "Tijolo ceramico", "Ceramica Horizonte",
        "10 milheiro", 8_200, "27/06/2026", STATUS_ENTREGUE, "Lucas Ferreira"));
    lista.add(new CompraBase(id++, 4, "Argamassa AC-III", "Argamassas PR",
        "60 sacas", 4_350, "01/07/2026", STATUS_PENDENTE, "Beatriz Almeida"));
    lista.add(new CompraBase(id++, 5, "Tinta latex", "Suvinil",
        "30 latas", 2_940, "30/06/2026", STATUS_APROVADO, "Ricardo Gomes"));
    lista.add(new CompraBase(id++, 5, "Piso vinilico", "Pisos & Revest.",
        "200 m2", 14_800, "26/06/2026", STATUS_ENTREGUE, "Ricardo Gomes"));
    lista.add(new CompraBase(id++, 2, "EPI - luvas e capacetes", "Seguranca Total",
        "80 kits", 3_200, "15/06/2026", STATUS_CANCELADO, "Roberto Lima"));
    lista.add(new CompraBase(id++, 1, "Areia media", "Areias Curitiba",
        "50 m3", 6_500, "01/07/2026", STATUS_PENDENTE, "Carlos Alberto Mendes"));
    return Collections.unmodifiableList(lista);
  }

  private static void adicionarMateriaisCanteiro(List<MaterialBase> lista, int canteiroId, MaterialBase... itens) {
    lista.addAll(Arrays.asList(itens));
  }

  private static MaterialBase mat(int canteiroId, String nome, String unidade, double qtd, double min) {
    return new MaterialBase(canteiroId, nome, unidade, qtd, min);
  }

  private static final class CanteiroBase {
    private final String nome;
    private final String localizacao;
    private final double tempBase;
    private final double conclusaoBase;

    private CanteiroBase(String nome, String localizacao, double tempBase, double conclusaoBase) {
      this.nome = nome;
      this.localizacao = localizacao;
      this.tempBase = tempBase;
      this.conclusaoBase = conclusaoBase;
    }
  }

  private static final class FuncionarioBase {
    private final int id;
    private final String nome;
    private final String tipo;
    private final String funcao;
    private final int canteiroId;

    private FuncionarioBase(int id, String nome, String tipo, String funcao, int canteiroId) {
      this.id = id;
      this.nome = nome;
      this.tipo = tipo;
      this.funcao = funcao;
      this.canteiroId = canteiroId;
    }
  }

  private static final class MaterialBase {
    private final int canteiroId;
    private final String nome;
    private final String unidade;
    private final double quantidade;
    private final double quantidadeMinima;

    private MaterialBase(int canteiroId, String nome, String unidade, double quantidade, double quantidadeMinima) {
      this.canteiroId = canteiroId;
      this.nome = nome;
      this.unidade = unidade;
      this.quantidade = quantidade;
      this.quantidadeMinima = quantidadeMinima;
    }
  }

  private static final class FinancaBase {
    private final int canteiroId;
    private final double orcamentoTotal;
    private final double gastoAcumulado;
    private final double gastoMes;
    private final double folhaMes;
    private final double materiaisMes;

    private FinancaBase(int canteiroId, double orcamentoTotal, double gastoAcumulado,
        double gastoMes, double folhaMes, double materiaisMes) {
      this.canteiroId = canteiroId;
      this.orcamentoTotal = orcamentoTotal;
      this.gastoAcumulado = gastoAcumulado;
      this.gastoMes = gastoMes;
      this.folhaMes = folhaMes;
      this.materiaisMes = materiaisMes;
    }
  }

  private static final class CompraBase {
    private final int id;
    private final int canteiroId;
    private final String descricao;
    private final String fornecedor;
    private final String quantidade;
    private final double valor;
    private final String dataPedido;
    private final String status;
    private final String solicitante;

    private CompraBase(int id, int canteiroId, String descricao, String fornecedor,
        String quantidade, double valor, String dataPedido, String status, String solicitante) {
      this.id = id;
      this.canteiroId = canteiroId;
      this.descricao = descricao;
      this.fornecedor = fornecedor;
      this.quantidade = quantidade;
      this.valor = valor;
      this.dataPedido = dataPedido;
      this.status = status;
      this.solicitante = solicitante;
    }
  }
}
