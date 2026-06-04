package br.edu.utfpr;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import br.edu.utfpr.model.Canteiro;
import br.edu.utfpr.model.EPI;
import br.edu.utfpr.model.Trabalhador;
import br.edu.utfpr.service.CanteiroService;
import br.edu.utfpr.service.EPIService;
import br.edu.utfpr.service.ProcessadorFolhaPagamento;
import br.edu.utfpr.service.TrabalhadorService;
import br.edu.utfpr.util.BackupUtil;
import br.edu.utfpr.util.ExportacaoUtil;
import br.edu.utfpr.util.ExportadorBenchmark;
import br.edu.utfpr.util.JPAUtil;
import br.edu.utfpr.util.ValidacaoUtil;
import jakarta.persistence.EntityManager;

import com.sun.management.OperatingSystemMXBean;

public class MenuConsoleSimplificado {

    private static Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

    public static void definirEntrada(InputStream entrada) {
        scanner = new Scanner(entrada, StandardCharsets.UTF_8);
    }

    public static void restaurarEntradaPadrao() {
        scanner = new Scanner(System.in, StandardCharsets.UTF_8);
    }
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int CARGA_APRESENTACAO = 5_000_000;

    private record SnapshotRecursos(Double cpuProcesso, Double cpuSistema, Double ramFisicaPct, Double heapJvmPct) {
        String formatarLinha(String rotulo) {
            StringBuilder sb = new StringBuilder();
            if (rotulo != null && !rotulo.isBlank()) {
                sb.append(rotulo).append(" — ");
            }
            if (cpuProcesso != null) {
                sb.append(String.format("CPU (processo Java) ~%.0f%%", cpuProcesso * 100.0));
            } else if (cpuSistema != null) {
                sb.append(String.format("CPU (sistema) ~%.0f%%", cpuSistema * 100.0));
            } else {
                sb.append("CPU: n/d");
            }
            if (ramFisicaPct != null) {
                sb.append(String.format(" | RAM física em uso ~%.0f%%", ramFisicaPct));
            } else {
                sb.append(" | RAM física: n/d");
            }
            if (heapJvmPct != null) {
                sb.append(String.format(" | heap JVM ~%.0f%% do máximo", heapJvmPct));
            }
            sb.append(" (aprox.)");
            return sb.toString();
        }

        String celulaCpuCurta() {
            if (cpuProcesso != null) {
                return String.format("%.0f", cpuProcesso * 100.0);
            }
            if (cpuSistema != null) {
                return String.format("S%.0f", cpuSistema * 100.0);
            }
            return "n/d";
        }

        String celulaRamCurta() {
            return ramFisicaPct != null ? String.format("%.0f", ramFisicaPct) : "n/d";
        }
    }

    public static void main(String[] args) {
        exibirMenu();
    }

    public static void exibirMenu() {
        EntityManager em = JPAUtil.getEntityManager();

        TrabalhadorService trabalhadorService = new TrabalhadorService(em);
        CanteiroService canteiroService = new CanteiroService(em);
        EPIService epiService = new EPIService(em);

        boolean continuar = true;

        System.out.println("\n" + "=".repeat(60));
        System.out.println("     SISTEMA DE GESTÃO DE TRABALHO DECENTE");
        System.out.println("     Objetivo de Desenvolvimento Sustentável 8 (ODS 8)");
        System.out.println("=".repeat(60));

        while (continuar) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("                    MENU PRINCIPAL");
            System.out.println("=".repeat(60));
            System.out.println("1. 👷 Gerenciar Trabalhadores");
            System.out.println("2. 🏗️ Gerenciar Canteiros");
            System.out.println("3. 🦺 Gerenciar EPIs");
            System.out.println("4. 📊 Relatórios");
            System.out.println("5. 💾 Exportar Dados");
            System.out.println("6. 🔄 Backup e Restauração");
            System.out.println("7. ⏱️ Análise de Desempenho (Threads)");
            System.out.println("0. 🚪 Sair");
            System.out.println("=".repeat(60));
            System.out.print("Escolha uma opção: ");

            int opcao = lerOpcao();

            switch (opcao) {
                case 1:
                    menuTrabalhadores(trabalhadorService, canteiroService);
                    break;
                case 2:
                    menuCanteiros(canteiroService);
                    break;
                case 3:
                    menuEPIs(epiService, trabalhadorService);
                    break;
                case 4:
                    menuRelatorios(trabalhadorService, canteiroService, epiService);
                    break;
                case 5:
                    menuExportacao(trabalhadorService, canteiroService, epiService);
                    break;
                case 6:
                    menuBackup(trabalhadorService);
                    break;
                case 7:
                    executarAnaliseDeDesempenho();
                    break;
                case 0:
                    continuar = false;
                    System.out.println("\n👋 Encerrando sistema...");
                    break;
                default:
                    System.out.println("\n❌ Opção inválida!");
            }
        }

        em.close();
        // O scanner só deve ser fechado se o programa for encerrar completamente.
        // Se chamado por outra thread, fechar o System.in pode quebrar o console.
        // scanner.close();
    }

    private static void menuTrabalhadores(TrabalhadorService service, CanteiroService cService) {
        while (true) {
            System.out.println("\n" + "-".repeat(60));
            System.out.println("           GERENCIAR TRABALHADORES");
            System.out.println("-".repeat(60));
            System.out.println("1. Cadastrar Trabalhador");
            System.out.println("2. Listar Todos");
            System.out.println("3. Buscar por ID");
            System.out.println("4. Atualizar Trabalhador");
            System.out.println("5. Remover Trabalhador");
            System.out.println("6. Exemplos de Streams API");
            System.out.println("0. Voltar");
            System.out.println("-".repeat(60));
            System.out.print("Escolha uma opção: ");

            int opcao = lerOpcao();

            switch (opcao) {
                case 1:
                    cadastrarTrabalhador(service, cService);
                    break;
                case 2:
                    listarTrabalhadores(service);
                    break;
                case 3:
                    buscarTrabalhadorPorId(service);
                    break;
                case 4:
                    atualizarTrabalhador(service, cService);
                    break;
                case 5:
                    removerTrabalhador(service);
                    break;
                case 6:
                    exemploStreams(service);
                    break;
                case 0:
                    return;
                default:
                    System.out.println("\n❌ Opção inválida!");
            }
        }
    }

    private static void cadastrarTrabalhador(TrabalhadorService service, CanteiroService cService) {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("           CADASTRAR TRABALHADOR");
        System.out.println("-".repeat(60));

        Trabalhador novo = new Trabalhador();

        System.out.print("Nome Completo: ");
        novo.setNomeCompleto(scanner.nextLine());

        String cpf;
        while (true) {
            System.out.print("CPF (apenas números): ");
            cpf = scanner.nextLine();
            if (ValidacaoUtil.validarCPF(cpf)) {
                novo.setCpf(cpf);
                break;
            } else {
                System.out.println("❌ CPF inválido! Tente novamente.");
            }
        }

        System.out.print("Função: ");
        String funcao = scanner.nextLine();
        novo.setFuncao(funcao);

        if (ValidacaoUtil.isFuncaoRequerCREA(funcao)) {
            System.out.print("Número CREA: ");
            novo.setNumeroCREA(scanner.nextLine());
            System.out.print("Especialidade: ");
            novo.setEspecialidade(scanner.nextLine());
        } else if (ValidacaoUtil.isFuncaoRequerRegistro(funcao)) {
            System.out.print("Número de Registro Profissional: ");
            novo.setNumeroRegistroProfissional(scanner.nextLine());
        }

        novo.setDataContratacao(LocalDate.now());

        System.out.print("Tipo de Contrato (CLT/PJ/TEMPORARIO): ");
        novo.setTipoContrato(scanner.nextLine());

        System.out.print("Associar a canteiro? (S/N): ");
        if (scanner.nextLine().equalsIgnoreCase("S")) {
            List<Canteiro> canteiros = cService.buscarTodos();
            if (!canteiros.isEmpty()) {
                System.out.println("\nCanteiros disponíveis:");
                canteiros.forEach(c -> System.out.println(c.getId() + " - " + c.getNome()));
                System.out.print("ID do canteiro: ");
                Long canteiroId = lerLong();
                Canteiro canteiro = cService.buscarPorId(canteiroId);
                if (canteiro != null) {
                    novo.setCanteiroAtual(canteiro);
                }
            }
        }

        service.inserir(novo);
        System.out.println("\n✅ Trabalhador cadastrado com sucesso! ID: " + novo.getId());
    }

    private static void listarTrabalhadores(TrabalhadorService service) {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("           LISTA DE TRABALHADORES");
        System.out.println("-".repeat(60));

        List<Trabalhador> todos = service.buscarTodos();
        if (todos.isEmpty()) {
            System.out.println("⚠️  Nenhum trabalhador cadastrado.");
        } else {
            for (Trabalhador t : todos) {
                System.out.println("\nID: " + t.getId());
                System.out.println("Nome: " + t.getNomeCompleto());
                System.out.println("CPF: " + t.getCpf());
                System.out.println("Função: " + t.getFuncao());
                if (t.getNumeroCREA() != null) {
                    System.out.println("CREA: " + t.getNumeroCREA());
                }
                if (t.getNumeroRegistroProfissional() != null) {
                    System.out.println("Registro: " + t.getNumeroRegistroProfissional());
                }
                System.out.println("Contrato: " + t.getTipoContrato());
                if (t.getCanteiroAtual() != null) {
                    System.out.println("Canteiro: " + t.getCanteiroAtual().getNome());
                }
                System.out.println("-".repeat(60));
            }
            System.out.println("Total: " + todos.size() + " trabalhadores");
        }
    }

    private static void buscarTrabalhadorPorId(TrabalhadorService service) {
        System.out.print("\nDigite o ID: ");
        Long id = lerLong();
        Trabalhador t = service.buscarPorId(id);
        if (t != null) {
            System.out.println("\n" + t.toString());
        } else {
            System.out.println("\n❌ Trabalhador não encontrado!");
        }
    }

    private static void atualizarTrabalhador(TrabalhadorService service, CanteiroService cService) {
        System.out.print("\nID do trabalhador: ");
        Long id = lerLong();
        Trabalhador t = service.buscarPorId(id);

        if (t != null) {
            System.out.println("Trabalhador atual: " + t.getNomeCompleto());
            if (t.getCanteiroAtual() != null) {
                System.out.println("Canteiro atual: " + t.getCanteiroAtual().getNome());
            } else {
                System.out.println("Canteiro atual: Nenhum");
            }

            System.out.print("Nova função (vazio = manter): ");
            String funcao = scanner.nextLine();
            if (!funcao.trim().isEmpty()) {
                t.setFuncao(funcao);
            }

            System.out.print("Novo tipo de contrato (vazio = manter): ");
            String contrato = scanner.nextLine();
            if (!contrato.trim().isEmpty()) {
                t.setTipoContrato(contrato);
            }

            System.out.print("Alterar canteiro? (S/N): ");
            if (scanner.nextLine().equalsIgnoreCase("S")) {
                List<Canteiro> canteiros = cService.buscarTodos();
                if (!canteiros.isEmpty()) {
                    System.out.println("\nCanteiros disponíveis:");
                    System.out.println("0 - Remover de canteiro");
                    canteiros.forEach(c -> System.out.println(c.getId() + " - " + c.getNome()));
                    System.out.print("ID do canteiro (0 para remover): ");
                    Long canteiroId = lerLong();
                    if (canteiroId == 0) {
                        t.setCanteiroAtual(null);
                        System.out.println("✅ Trabalhador removido do canteiro");
                    } else {
                        Canteiro canteiro = cService.buscarPorId(canteiroId);
                        if (canteiro != null) {
                            t.setCanteiroAtual(canteiro);
                            System.out.println("✅ Canteiro alterado para: " + canteiro.getNome());
                        } else {
                            System.out.println("⚠️  Canteiro não encontrado, mantendo anterior");
                        }
                    }
                } else {
                    System.out.println("⚠️  Nenhum canteiro cadastrado");
                }
            }

            service.atualizar(t);
            System.out.println("\n✅ Trabalhador atualizado!");
        } else {
            System.out.println("\n❌ Trabalhador não encontrado!");
        }
    }

    private static void removerTrabalhador(TrabalhadorService service) {
        System.out.print("\nID do trabalhador: ");
        Long id = lerLong();
        Trabalhador t = service.buscarPorId(id);

        if (t != null) {
            System.out.print("Confirma remoção de " + t.getNomeCompleto() + "? (S/N): ");
            if (scanner.nextLine().equalsIgnoreCase("S")) {
                service.remover(id);
                System.out.println("\n✅ Trabalhador removido!");
            }
        } else {
            System.out.println("\n❌ Trabalhador não encontrado!");
        }
    }

    private static void exemploStreams(TrabalhadorService service) {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("           EXEMPLOS DE STREAMS API");
        System.out.println("-".repeat(60));
        System.out.println("1. Filtrar por função");
        System.out.println("2. Buscar por CPF");
        System.out.println("3. Contar por tipo de contrato");
        System.out.println("4. Listar apenas nomes");
        System.out.println("5. Calcular estatísticas");
        System.out.println("0. Voltar");
        System.out.println("-".repeat(60));
        System.out.print("Escolha: ");

        int opcao = lerOpcao();

        switch (opcao) {
            case 1:
                System.out.print("\nFunção: ");
                String funcao = scanner.nextLine();
                service.filtrarPorFuncao(funcao).forEach(t ->
                        System.out.println("• " + t.getNomeCompleto() + " - " + t.getFuncao())
                );
                break;
            case 2:
                System.out.print("\nCPF: ");
                String cpf = scanner.nextLine();
                service.buscarPorCPF(cpf).ifPresentOrElse(
                        t -> System.out.println("✅ Encontrado: " + t.getNomeCompleto()),
                        () -> System.out.println("❌ Não encontrado")
                );
                break;
            case 3:
                System.out.println("\nContagem por tipo de contrato:");
                service.contarPorTipoContrato().forEach((tipo, count) ->
                        System.out.println("• " + tipo + ": " + count)
                );
                break;
            case 4:
                System.out.println("\nNomes dos trabalhadores:");
                service.listarNomes().forEach(nome -> System.out.println("• " + nome));
                break;
            case 5:
                System.out.println("\nEstatísticas:");
                System.out.println("Total: " + service.buscarTodos().size());
                System.out.println("Com CREA: " + service.buscarTodos().stream()
                        .filter(t -> t.getNumeroCREA() != null).count());
                break;
        }
    }

    private static void menuCanteiros(CanteiroService service) {
        while (true) {
            System.out.println("\n" + "-".repeat(60));
            System.out.println("           GERENCIAR CANTEIROS");
            System.out.println("-".repeat(60));
            System.out.println("1. Cadastrar Canteiro");
            System.out.println("2. Listar Todos");
            System.out.println("3. Buscar por ID");
            System.out.println("4. Atualizar Canteiro");
            System.out.println("5. Remover Canteiro");
            System.out.println("0. Voltar");
            System.out.println("-".repeat(60));
            System.out.print("Escolha uma opção: ");

            int opcao = lerOpcao();

            switch (opcao) {
                case 1:
                    cadastrarCanteiro(service);
                    break;
                case 2:
                    listarCanteiros(service);
                    break;
                case 3:
                    buscarCanteiroPorId(service);
                    break;
                case 4:
                    atualizarCanteiro(service);
                    break;
                case 5:
                    removerCanteiro(service);
                    break;
                case 0:
                    return;
                default:
                    System.out.println("\n❌ Opção inválida!");
            }
        }
    }

    private static void cadastrarCanteiro(CanteiroService service) {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("           CADASTRAR CANTEIRO");
        System.out.println("-".repeat(60));

        Canteiro novo = new Canteiro();

        System.out.print("Nome do Canteiro: ");
        novo.setNome(scanner.nextLine());

        System.out.print("Localização: ");
        novo.setLocalizacao(scanner.nextLine());

        System.out.print("Responsável: ");
        novo.setResponsavel(scanner.nextLine());

        novo.setDataInicio(LocalDate.now());

        System.out.print("Data Previsão Término (dd/MM/yyyy): ");
        try {
            LocalDate dataTermino = LocalDate.parse(scanner.nextLine(), dateFormatter);
            novo.setDataPrevisaoTermino(dataTermino);
        } catch (DateTimeParseException e) {
            System.out.println("⚠️  Data inválida, usando 6 meses a partir de hoje.");
            novo.setDataPrevisaoTermino(LocalDate.now().plusMonths(6));
        }

        service.inserir(novo);
        System.out.println("\n✅ Canteiro cadastrado! ID: " + novo.getId());
    }

    private static void listarCanteiros(CanteiroService service) {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("           LISTA DE CANTEIROS");
        System.out.println("-".repeat(60));

        List<Canteiro> todos = service.buscarTodos();
        if (todos.isEmpty()) {
            System.out.println("⚠️  Nenhum canteiro cadastrado.");
        } else {
            for (Canteiro c : todos) {
                System.out.println("\nID: " + c.getId());
                System.out.println("Nome: " + c.getNome());
                System.out.println("Localização: " + c.getLocalizacao());
                System.out.println("Responsável: " + c.getResponsavel());
                System.out.println("Início: " + c.getDataInicio().format(dateFormatter));
                System.out.println("Previsão Término: " + c.getDataPrevisaoTermino().format(dateFormatter));
                System.out.println("-".repeat(60));
            }
            System.out.println("Total: " + todos.size() + " canteiros");
        }
    }

    private static void buscarCanteiroPorId(CanteiroService service) {
        System.out.print("\nDigite o ID: ");
        Long id = lerLong();
        Canteiro c = service.buscarPorId(id);
        if (c != null) {
            System.out.println("\n" + c.toString());
        } else {
            System.out.println("\n❌ Canteiro não encontrado!");
        }
    }

    private static void atualizarCanteiro(CanteiroService service) {
        System.out.print("\nID do canteiro: ");
        Long id = lerLong();
        Canteiro c = service.buscarPorId(id);

        if (c != null) {
            System.out.println("Canteiro atual: " + c.getNome());
            System.out.print("Novo responsável (vazio = manter): ");
            String resp = scanner.nextLine();
            if (!resp.trim().isEmpty()) {
                c.setResponsavel(resp);
            }

            service.atualizar(c);
            System.out.println("\n✅ Canteiro atualizado!");
        } else {
            System.out.println("\n❌ Canteiro não encontrado!");
        }
    }

    private static void removerCanteiro(CanteiroService service) {
        System.out.print("\nID do canteiro: ");
        Long id = lerLong();
        Canteiro c = service.buscarPorId(id);

        if (c != null) {
            System.out.print("Confirma remoção de " + c.getNome() + "? (S/N): ");
            if (scanner.nextLine().equalsIgnoreCase("S")) {
                service.remover(id);
                System.out.println("\n✅ Canteiro removido!");
            }
        } else {
            System.out.println("\n❌ Canteiro não encontrado!");
        }
    }

    private static void menuEPIs(EPIService service, TrabalhadorService tService) {
        while (true) {
            System.out.println("\n" + "-".repeat(60));
            System.out.println("           GERENCIAR EPIs");
            System.out.println("-".repeat(60));
            System.out.println("1. Cadastrar EPI");
            System.out.println("2. Listar Todos");
            System.out.println("3. Buscar por ID");
            System.out.println("4. Atualizar EPI");
            System.out.println("5. Remover EPI");
            System.out.println("0. Voltar");
            System.out.println("-".repeat(60));
            System.out.print("Escolha uma opção: ");

            int opcao = lerOpcao();

            switch (opcao) {
                case 1:
                    cadastrarEPI(service, tService);
                    break;
                case 2:
                    listarEPIs(service);
                    break;
                case 3:
                    buscarEPIPorId(service);
                    break;
                case 4:
                    atualizarEPI(service);
                    break;
                case 5:
                    removerEPI(service);
                    break;
                case 0:
                    return;
                default:
                    System.out.println("\n❌ Opção inválida!");
            }
        }
    }

    private static void cadastrarEPI(EPIService service, TrabalhadorService tService) {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("           CADASTRAR EPI");
        System.out.println("-".repeat(60));

        EPI novo = new EPI();

        System.out.print("Tipo (Capacete/Luva/Bota/Óculos/etc): ");
        novo.setTipo(scanner.nextLine());

        System.out.print("Número CA: ");
        novo.setNumeroCA(scanner.nextLine());

        novo.setDataEntrega(LocalDate.now());

        System.out.print("Data Validade (dd/MM/yyyy): ");
        try {
            LocalDate validade = LocalDate.parse(scanner.nextLine(), dateFormatter);
            novo.setDataValidade(validade);
        } catch (DateTimeParseException e) {
            System.out.println("⚠️  Data inválida, usando 1 ano a partir de hoje.");
            novo.setDataValidade(LocalDate.now().plusYears(1));
        }

        System.out.print("Associar a trabalhador? (S/N): ");
        if (scanner.nextLine().equalsIgnoreCase("S")) {
            List<Trabalhador> trabalhadores = tService.buscarTodos();
            if (!trabalhadores.isEmpty()) {
                System.out.println("\nTrabalhadores:");
                trabalhadores.forEach(t -> System.out.println(t.getId() + " - " + t.getNomeCompleto()));
                System.out.print("ID: ");
                Long tid = lerLong();
                Trabalhador t = tService.buscarPorId(tid);
                if (t != null) {
                    novo.setTrabalhador(t);
                }
            }
        }

        service.inserir(novo);
        System.out.println("\n✅ EPI cadastrado! ID: " + novo.getId());
    }

    private static void listarEPIs(EPIService service) {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("           LISTA DE EPIs");
        System.out.println("-".repeat(60));

        List<EPI> todos = service.buscarTodos();
        if (todos.isEmpty()) {
            System.out.println("⚠️  Nenhum EPI cadastrado.");
        } else {
            for (EPI e : todos) {
                System.out.println("\nID: " + e.getId());
                System.out.println("Tipo: " + e.getTipo());
                System.out.println("CA: " + e.getNumeroCA());
                System.out.println("Entrega: " + e.getDataEntrega().format(dateFormatter));
                System.out.println("Validade: " + e.getDataValidade().format(dateFormatter));
                if (e.getDataValidade().isBefore(LocalDate.now())) {
                    System.out.println("⚠️  VENCIDO!");
                }
                if (e.getTrabalhador() != null) {
                    System.out.println("Trabalhador: " + e.getTrabalhador().getNomeCompleto());
                }
                System.out.println("-".repeat(60));
            }
            System.out.println("Total: " + todos.size() + " EPIs");
        }
    }

    private static void buscarEPIPorId(EPIService service) {
        System.out.print("\nDigite o ID: ");
        Long id = lerLong();
        EPI e = service.buscarPorId(id);
        if (e != null) {
            System.out.println("\n" + e.toString());
        } else {
            System.out.println("\n❌ EPI não encontrado!");
        }
    }

    private static void atualizarEPI(EPIService service) {
        System.out.print("\nID do EPI: ");
        Long id = lerLong();
        EPI e = service.buscarPorId(id);

        if (e != null) {
            System.out.println("EPI atual: " + e.getTipo());
            System.out.print("Nova validade (dd/MM/yyyy, vazio = manter): ");
            String data = scanner.nextLine();
            if (!data.trim().isEmpty()) {
                try {
                    e.setDataValidade(LocalDate.parse(data, dateFormatter));
                } catch (DateTimeParseException ex) {
                    System.out.println("⚠️  Data inválida, mantendo anterior.");
                }
            }

            service.atualizar(e);
            System.out.println("\n✅ EPI atualizado!");
        } else {
            System.out.println("\n❌ EPI não encontrado!");
        }
    }

    private static void removerEPI(EPIService service) {
        System.out.print("\nID do EPI: ");
        Long id = lerLong();
        EPI e = service.buscarPorId(id);

        if (e != null) {
            System.out.print("Confirma remoção do EPI " + e.getTipo() + "? (S/N): ");
            if (scanner.nextLine().equalsIgnoreCase("S")) {
                service.remover(id);
                System.out.println("\n✅ EPI removido!");
            }
        } else {
            System.out.println("\n❌ EPI não encontrado!");
        }
    }

    private static void menuRelatorios(TrabalhadorService tService, CanteiroService cService, EPIService eService) {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("           RELATÓRIOS");
        System.out.println("-".repeat(60));
        System.out.println("1. Trabalhadores por Canteiro");
        System.out.println("2. EPIs Vencidos");
        System.out.println("3. Estatísticas Gerais");
        System.out.println("0. Voltar");
        System.out.println("-".repeat(60));
        System.out.print("Escolha: ");

        int opcao = lerOpcao();

        switch (opcao) {
            case 1:
                relatorioTrabalhadoresPorCanteiro(tService);
                break;
            case 2:
                relatorioEPIsVencidos(eService);
                break;
            case 3:
                relatorioEstatisticas(tService, cService, eService);
                break;
        }
    }

    private static void relatorioTrabalhadoresPorCanteiro(TrabalhadorService service) {
        System.out.println("\n📊 TRABALHADORES POR CANTEIRO");
        System.out.println("-".repeat(60));

        service.buscarTodos().stream()
                .filter(t -> t.getCanteiroAtual() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        t -> t.getCanteiroAtual().getNome(),
                        java.util.stream.Collectors.toList()
                ))
                .forEach((canteiro, trabalhadores) -> {
                    System.out.println("\n🏗️  " + canteiro + " (" + trabalhadores.size() + " trabalhadores)");
                    trabalhadores.forEach(t -> System.out.println("  • " + t.getNomeCompleto() + " - " + t.getFuncao()));
                });
    }

    private static void relatorioEPIsVencidos(EPIService service) {
        System.out.println("\n📊 EPIs VENCIDOS");
        System.out.println("-".repeat(60));

        List<EPI> vencidos = service.buscarTodos().stream()
                .filter(e -> e.getDataValidade().isBefore(LocalDate.now()))
                .toList();

        if (vencidos.isEmpty()) {
            System.out.println("✅ Nenhum EPI vencido!");
        } else {
            vencidos.forEach(e -> {
                System.out.println("\n⚠️  " + e.getTipo() + " (CA: " + e.getNumeroCA() + ")");
                System.out.println("   Vencido em: " + e.getDataValidade().format(dateFormatter));
                if (e.getTrabalhador() != null) {
                    System.out.println("   Trabalhador: " + e.getTrabalhador().getNomeCompleto());
                }
            });
            System.out.println("\nTotal: " + vencidos.size() + " EPIs vencidos");
        }
    }

    private static void relatorioEstatisticas(TrabalhadorService tService, CanteiroService cService, EPIService eService) {
        System.out.println("\n📊 ESTATÍSTICAS GERAIS");
        System.out.println("-".repeat(60));
        System.out.println("👷 Trabalhadores: " + tService.buscarTodos().size());
        System.out.println("🏗️  Canteiros: " + cService.buscarTodos().size());
        System.out.println("🦺 EPIs: " + eService.buscarTodos().size());

        long comCREA = tService.buscarTodos().stream()
                .filter(t -> t.getNumeroCREA() != null)
                .count();
        System.out.println("📋 Com CREA: " + comCREA);

        long episVencidos = eService.buscarTodos().stream()
                .filter(e -> e.getDataValidade().isBefore(LocalDate.now()))
                .count();
        System.out.println("⚠️  EPIs Vencidos: " + episVencidos);
    }

    private static void menuExportacao(TrabalhadorService tService, CanteiroService cService, EPIService eService) {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("           EXPORTAR DADOS");
        System.out.println("-".repeat(60));
        System.out.println("1. Exportar Trabalhadores");
        System.out.println("2. Exportar Canteiros");
        System.out.println("3. Exportar EPIs");
        System.out.println("0. Voltar");
        System.out.println("-".repeat(60));
        System.out.print("Escolha: ");

        int opcao = lerOpcao();

        if (opcao >= 1 && opcao <= 3) {
            System.out.println("\nFormato:");
            System.out.println("1. TXT");
            System.out.println("2. JSON");
            System.out.println("3. BIN");
            System.out.print("Escolha: ");
            int formato = lerOpcao();

            System.out.print("\nNome do arquivo (sem extensão): ");
            String nome = scanner.nextLine();

            try {
                switch (opcao) {
                    case 1:
                        exportarDados(tService.buscarTodos(), nome, formato, "Trabalhadores");
                        break;
                    case 2:
                        exportarDados(cService.buscarTodos(), nome, formato, "Canteiros");
                        break;
                    case 3:
                        exportarDados(eService.buscarTodos(), nome, formato, "EPIs");
                        break;
                }
            } catch (Exception e) {
                System.out.println("❌ Erro ao exportar: " + e.getMessage());
            }
        }
    }

    private static void exportarDados(List<?> dados, String nome, int formato, String tipo) throws Exception {
        String caminho = "exports/" + nome;

        switch (formato) {
            case 1:
                ExportacaoUtil.exportarParaTxt(dados, caminho + ".txt", tipo);
                break;
            case 2:
                ExportacaoUtil.exportarParaJson(dados, caminho + ".json");
                break;
            case 3:
                ExportacaoUtil.exportarParaBin(dados, caminho + ".bin");
                break;
        }

        System.out.println("\n✅ Dados exportados com sucesso!");
        System.out.println("📁 Arquivo: " + caminho);
    }


    private static void menuBackup(TrabalhadorService service) {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("           BACKUP E RESTAURAÇÃO");
        System.out.println("-".repeat(60));
        System.out.println("1. Fazer Backup");
        System.out.println("2. Restaurar Backup");
        System.out.println("0. Voltar");
        System.out.println("-".repeat(60));
        System.out.print("Escolha: ");

        int opcao = lerOpcao();

        switch (opcao) {
            case 1:
                try {
                    BackupUtil.fazerBackup(service.buscarTodos(), "backups/trabalhadores.bak");
                    System.out.println("\n✅ Backup realizado com sucesso!");
                } catch (Exception e) {
                    System.out.println("❌ Erro ao fazer backup: " + e.getMessage());
                }
                break;
            case 2:
                try {
                    List<Trabalhador> restaurados = BackupUtil.restaurarBackup("backups/trabalhadores.bak");
                    System.out.println("\n✅ Backup restaurado! " + restaurados.size() + " registros");
                } catch (Exception e) {
                    System.out.println("❌ Erro ao restaurar: " + e.getMessage());
                }
                break;
        }
    }


    private static int lerOpcao() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static Long lerLong() {
        try {
            return Long.parseLong(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private static long calcularMedia(long[] valores) {
        long soma = 0L;
        for (long v : valores) {
            soma += v;
        }
        return soma / valores.length;
    }

    private static long calcularMediana(long[] valores) {
        long[] copia = Arrays.copyOf(valores, valores.length);
        Arrays.sort(copia);
        int meio = copia.length / 2;
        if (copia.length % 2 == 0) {
            return (copia[meio - 1] + copia[meio]) / 2;
        }
        return copia[meio];
    }

    private static String formatarTempoDetalhado(long ms) {
        double segundos = ms / 1000.0;
        long minutosInteiros = ms / 60000;
        double segundosRestantes = (ms % 60000) / 1000.0;
        return String.format("%,d ms (%.2f s | %d min %.2f s)", ms, segundos, minutosInteiros, segundosRestantes);
    }

    private static long medirExecucaoSerialComRepeticoes(ProcessadorFolhaPagamento processador, int qtdLotes, int repeticoes) {
        long[] medidas = new long[repeticoes];
        for (int i = 0; i < repeticoes; i++) {
            medidas[i] = processador.executarSerialComTabela(qtdLotes, false);
        }
        return calcularMediana(medidas);
    }

    /**
     * Executa o paralelo N vezes (média). Imprime antes/depois de cada execução — durante o processamento
     * o console fica sem atualizar, o que é esperado com cargas grandes.
     */
    private static long medirExecucaoParalelaComRepeticoes(ProcessadorFolhaPagamento processador, int numThreads,
                                                           int repeticoes, String etiqueta) {
        long[] medidas = new long[repeticoes];
        for (int i = 0; i < repeticoes; i++) {
            System.out.printf("  » %s — iniciando execução %d/%d (%d thread(s), %,d registros). Aguarde…%n",
                    etiqueta, i + 1, repeticoes, numThreads, processador.getTotalRegistros());
            System.out.println("    (Enquanto processa, não aparecem novas linhas; isso é normal.)");
            System.out.flush();
            long t0 = System.currentTimeMillis();
            medidas[i] = processador.executarParaleloVariavel(numThreads);
            long decorrido = System.currentTimeMillis() - t0;
            System.out.printf("  » %s — execução %d/%d finalizada em %s (medido nesta repetição: %,d ms).%n",
                    etiqueta, i + 1, repeticoes, formatarTempoDetalhado(decorrido), medidas[i]);
            System.out.flush();
        }
        long media = calcularMedia(medidas);
        if (repeticoes > 1) {
            System.out.printf("  » %s — média das %d execuções: %,d ms (%s).%n",
                    etiqueta, repeticoes, media, formatarTempoDetalhado(media));
            System.out.flush();
        }
        return media;
    }

    /**
     * Pequena pausa para a JVM/OS atualizarem a leitura de CPU (valores iniciais costumam ser -1).
     */
    private static void prepararLeituraRecursos() {
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static OperatingSystemMXBean obterOperatingSystemBeanEstendido() {
        var bean = ManagementFactory.getOperatingSystemMXBean();
        if (bean instanceof OperatingSystemMXBean sun) {
            return sun;
        }
        return null;
    }

    private static SnapshotRecursos capturarSnapshotRecursos() {
        prepararLeituraRecursos();
        OperatingSystemMXBean sun = obterOperatingSystemBeanEstendido();
        Double cpuP = null;
        Double cpuS = null;
        Double ramPct = null;
        Double heapPct = null;
        if (sun != null) {
            double p = sun.getProcessCpuLoad();
            if (p >= 0) {
                cpuP = p;
            }
            double s = sun.getSystemCpuLoad();
            if (s >= 0) {
                cpuS = s;
            }
            long totalFis = sun.getTotalPhysicalMemorySize();
            long livreFis = sun.getFreePhysicalMemorySize();
            if (totalFis > 0) {
                ramPct = 100.0 * (totalFis - livreFis) / (double) totalFis;
            }
        }
        Runtime rt = Runtime.getRuntime();
        long heapMax = rt.maxMemory();
        if (heapMax > 0) {
            heapPct = 100.0 * (rt.totalMemory() - rt.freeMemory()) / (double) heapMax;
        }
        return new SnapshotRecursos(cpuP, cpuS, ramPct, heapPct);
    }

    private static String formatarLinhaRecursos(String rotulo) {
        return capturarSnapshotRecursos().formatarLinha(rotulo);
    }

    private static void imprimirExplicacaoModeloFolha() {
        System.out.println("\n--- O que este benchmark simula (regras simplificadas, fins didáticos) ---");
        System.out.println("• Cada registro = um empregado com salário base aleatório.");
        System.out.println("• A folha é anual (12 meses) com sorteios por ID/mês (reexecução reproduz o mesmo cenário).");
        System.out.println("• Por mês: variação sazonal no salário; faltas (0 a 3) com desconto = (salário do mês ÷ 30) × faltas;");
        System.out.println("  afastamento: sorteia dias no mês; cada dia afastado paga 60% do valor de um dia, os demais 100% (base 30 dias);");
        System.out.println("  hora extra e feriado trabalhado geram adicionais;");
        System.out.println("  INSS, IRRF, FGTS e um termo periculosidade (Math.pow) compõem o líquido;");
        System.out.println("• Em dezembro: abono tipo 13º simplificado (50% do salário base);");
        System.out.println("• Demissão/rescisão: desconto simplificado (40% do FGTS do mês) e encerramento dos meses seguintes.");
    }

    private static void executarAnaliseDeDesempenho() {
        System.out.println("\n============================================================");
        System.out.println("   ANÁLISE DE DESEMPENHO COMPUTACIONAL (SERIAL VS PARALELO)");
        System.out.println("============================================================");

        imprimirExplicacaoModeloFolha();

        System.out.println("\nModo de execução:");
        System.out.println("1. Completo — " + String.format("%,d", ProcessadorFolhaPagamento.TOTAL_REGISTROS)
                + " registros, até 16 threads (relatório / professor)");
        System.out.println("2. Apresentação rápida — " + String.format("%,d", CARGA_APRESENTACAO)
                + " registros, até 8 threads (demonstração em sala)");
        System.out.print("Opção: ");
        int modoExec = lerOpcao();

        int cargaRegistros = ProcessadorFolhaPagamento.TOTAL_REGISTROS;
        int maxThreads = 16;
        if (modoExec == 2) {
            cargaRegistros = CARGA_APRESENTACAO;
            maxThreads = 8;
        }

        System.out.println("\nCONFIGURAÇÃO DA EXECUÇÃO SERIAL");
        System.out.printf("Para demonstrar o processamento em fila única, dividiremos os %,d registros em lotes.%n",
                cargaRegistros);
        System.out.println("- Menos lotes: menos atualização na tela.");
        System.out.println("- Mais lotes: mais linhas, feedback mais frequente.");
        System.out.println("Recomendação: entre 5 e 20 lotes (no modo rápido, ~8 lotes costuma ser confortável).");

        System.out.print("\nQuantos lotes para a execução serial (ex.: 15 = 15 lotes, de 1 a 15)? ");
        int qtdLotes = lerOpcao();
        if (qtdLotes <= 0) {
            qtdLotes = (modoExec == 2) ? 8 : 10;
        }

        System.out.print("Quantas repetições por medição (completo: 3 a 5; apresentação: 1)? ");
        int repeticoes = lerOpcao();
        if (repeticoes <= 0) {
            repeticoes = (modoExec == 2) ? 1 : 3;
        }

        System.out.println("\nEscolha o formato da tabela de resultados:");
        System.out.println("1. Visual (Terminal)");
        System.out.println("2. Tabulação (Excel)");
        System.out.println("3. Ambos");
        System.out.print("Opção: ");
        int formatoTabela = lerOpcao();

        System.out.println("\nProblema: cálculo anual da folha com eventos (faltas, afastamentos, demissões e rescisões).");
        System.out.printf("Carga de trabalho: %,d registros na memória.%n", cargaRegistros);
        System.out.printf("Metodologia: %d repetições (serial = mediana das tabelas; paralelo = média).%n", repeticoes);
        System.out.printf("Threads avaliadas: 1 a %d.%n", maxThreads);
        System.out.println("Aguarde, processando...\n");

        long tempoInicioProcessamento = System.currentTimeMillis();
        ProcessadorFolhaPagamento processador = new ProcessadorFolhaPagamento(cargaRegistros);
        System.out.println(formatarLinhaRecursos("Após carregar dados na memória"));

        long tempoSerial = medirExecucaoSerialComRepeticoes(processador, qtdLotes, repeticoes);
        System.out.printf("%nTempo de execução SERIAL (mediana de %d repetições): %s%n", repeticoes,
                formatarTempoDetalhado(tempoSerial));
        System.out.println(formatarLinhaRecursos("Após execução serial"));

        System.out.println("\nIniciando benchmark paralelo com 2 threads...");
        System.out.println("Esta etapa percorre de novo todos os registros em paralelo; em carga grande pode levar muito tempo sem nova linha.");
        long tempoParaleloDuas = medirExecucaoParalelaComRepeticoes(processador, 2, repeticoes, "Paralelo 2 threads");
        System.out.println("Tempo de execução PARALELO (2 threads): " + formatarTempoDetalhado(tempoParaleloDuas));
        System.out.println(formatarLinhaRecursos("Após paralelo (2 threads)"));

        System.out.println("\nCONCLUSÃO (serial vs 2 threads):");
        if (tempoParaleloDuas < tempoSerial) {
            double ganho = ((double) tempoSerial / tempoParaleloDuas);
            System.out.printf("A versão com 2 threads foi %.2fx rápida que o serial.%n", ganho);
        } else {
            System.out.println("Não houve ganho expressivo com 2 threads nesta medição.");
        }

        System.out.println("\n============================================================");
        System.out.printf("   SPEED-UP (1 A %d THREADS)%n", maxThreads);
        System.out.println("============================================================\n");
        System.out.println("Calculando dados...");

        long[] tempos = new long[maxThreads + 1];
        double[] speedUps = new double[maxThreads + 1];
        double[] eficiencias = new double[maxThreads + 1];
        SnapshotRecursos[] snaps = new SnapshotRecursos[maxThreads + 1];
        Double[] cpuColCsv = new Double[maxThreads + 1];
        Double[] ramColCsv = new Double[maxThreads + 1];

        tempos[1] = tempoSerial;
        speedUps[1] = 1.00;
        eficiencias[1] = 100.00;
        snaps[1] = capturarSnapshotRecursos();
        cpuColCsv[1] = snaps[1].cpuProcesso() != null ? snaps[1].cpuProcesso() * 100.0
                : snaps[1].cpuSistema() != null ? snaps[1].cpuSistema() * 100.0 : null;
        ramColCsv[1] = snaps[1].ramFisicaPct();
        System.out.printf("[1/%d] Reaproveitando 1 thread: %s | SpeedUp: %.2f | Eficiência: %.2f%%%n",
                maxThreads, formatarTempoDetalhado(tempos[1]), speedUps[1], eficiencias[1]);
        System.out.println("    " + snaps[1].formatarLinha(null));

        tempos[2] = tempoParaleloDuas;
        speedUps[2] = (double) tempoSerial / tempos[2];
        eficiencias[2] = (speedUps[2] / 2) * 100.0;
        snaps[2] = capturarSnapshotRecursos();
        cpuColCsv[2] = snaps[2].cpuProcesso() != null ? snaps[2].cpuProcesso() * 100.0
                : snaps[2].cpuSistema() != null ? snaps[2].cpuSistema() * 100.0 : null;
        ramColCsv[2] = snaps[2].ramFisicaPct();
        System.out.printf("[2/%d] Reaproveitando 2 threads: %s | SpeedUp: %.2f | Eficiência: %.2f%%%n",
                maxThreads, formatarTempoDetalhado(tempos[2]), speedUps[2], eficiencias[2]);
        System.out.println("    " + snaps[2].formatarLinha(null));

        for (int i = 3; i <= maxThreads; i++) {
            System.out.printf("[%d/%d] Iniciando benchmark com %d threads...%n", i, maxThreads, i);
            tempos[i] = medirExecucaoParalelaComRepeticoes(processador, i, repeticoes, i + " threads");
            speedUps[i] = (double) tempoSerial / tempos[i];
            eficiencias[i] = (speedUps[i] / i) * 100.0;
            snaps[i] = capturarSnapshotRecursos();
            cpuColCsv[i] = snaps[i].cpuProcesso() != null ? snaps[i].cpuProcesso() * 100.0
                    : snaps[i].cpuSistema() != null ? snaps[i].cpuSistema() * 100.0 : null;
            ramColCsv[i] = snaps[i].ramFisicaPct();
            System.out.printf("[%d/%d] Concluído: %s | SpeedUp: %.2f | Eficiência: %.2f%%%n",
                    i, maxThreads, formatarTempoDetalhado(tempos[i]), speedUps[i], eficiencias[i]);
            System.out.println("    " + snaps[i].formatarLinha(null));
        }

        long somaTemposParalelos = 0L;
        for (int i = 2; i <= maxThreads; i++) {
            somaTemposParalelos += tempos[i];
        }

        if (formatoTabela == 1 || formatoTabela == 3) {
            System.out.println("\n+---------+------------+---------+---------------+--------+--------+");
            System.out.println("| Threads | Tempo (ms) | SpeedUp | Eficiência %  | CPU %  | RAM %  |");
            System.out.println("+---------+------------+---------+---------------+--------+--------+");
            for (int i = 1; i <= maxThreads; i++) {
                System.out.printf("| %-7d | %-10d | %-7.2f | %-13.2f | %-6s | %-6s |%n",
                        i, tempos[i], speedUps[i], eficiencias[i],
                        snaps[i].celulaCpuCurta(), snaps[i].celulaRamCurta());
            }
            System.out.println("+---------+------------+---------+---------------+--------+--------+");
            System.out.printf("Soma dos tempos das medições paralelas (threads 2 a %d): %,d ms%n",
                    maxThreads, somaTemposParalelos);
            System.out.println("(É a soma de cada execução medida, não o tempo de um único cronômetro em paralelo.)");
            System.out.printf("Referência serial (1 thread): %s%n", formatarTempoDetalhado(tempos[1]));
            System.out.println(formatarLinhaRecursos("Após tabela de speed-up"));
        }

        if (formatoTabela == 2 || formatoTabela == 3) {
            System.out.println("\nThreads\tTempo(ms)\tSpeedUp\tEficiência(%)\tCPU(%)\tRAM(%)");
            for (int i = 1; i <= maxThreads; i++) {
                System.out.printf("%d\t%d\t%.2f\t%.2f\t%s\t%s%n",
                        i, tempos[i], speedUps[i], eficiencias[i],
                        snaps[i].celulaCpuCurta(), snaps[i].celulaRamCurta());
            }
            System.out.printf("SOMA_PARALELOS_2_%d_ms\t%d%n", maxThreads, somaTemposParalelos);
        }

        try {
            ExportadorBenchmark.exportarSpeedUpCsv(maxThreads, tempos, speedUps, eficiencias, cpuColCsv, ramColCsv);
            ExportadorBenchmark.exportarSpeedUpPng(maxThreads, speedUps);
            System.out.println("\nArquivos gerados em pasta \"exports\": speedup.csv e speedup.png (gráfico speed-up × threads).");
        } catch (Exception e) {
            System.out.println("\nNão foi possível exportar CSV/PNG: " + e.getMessage());
        }

        int melhorThread = 1;
        long melhorTempo = tempos[1];
        for (int i = 2; i <= maxThreads; i++) {
            if (tempos[i] < melhorTempo) {
                melhorTempo = tempos[i];
                melhorThread = i;
            }
        }

        System.out.println("\n--- Diagnóstico de desempenho ---");
        System.out.printf("Melhor configuração: %d threads (%,d ms).%n", melhorThread, melhorTempo);
        System.out.printf("Speed-up ideal em %d threads: %.2fx | Speed-up real: %.2fx%n",
                melhorThread, (double) melhorThread, speedUps[melhorThread]);
        System.out.printf("Diferença em relação ao ideal: %.2f%%%n",
                (100.0 * (melhorThread - speedUps[melhorThread])) / melhorThread);
        System.out.printf("Soma dos tempos das medições paralelas (2 a %d threads): %,d ms.%n",
                maxThreads, somaTemposParalelos);

        boolean houveRegressão = false;
        for (int i = 2; i <= maxThreads; i++) {
            if (tempos[i] > tempos[i - 1]) {
                if (!houveRegressão) {
                    System.out.println("Pontos de regressão (mais threads, tempo maior):");
                }
                houveRegressão = true;
                System.out.printf("  • de %d para %d threads: %,d ms -> %,d ms%n", (i - 1), i, tempos[i - 1], tempos[i]);
            }
        }
        if (!houveRegressão) {
            System.out.println("Sem regressão na faixa 1 a " + maxThreads + " threads.");
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("   RESULTADOS DA SIMULAÇÃO (FOLHA E EVENTOS)");
        System.out.println("=".repeat(60));
        processador.imprimirEstatisticas();

        System.out.print("\nTamanho da amostra para ranking de eventos (vazio = padrão): ");
        String linhaAmostra = scanner.nextLine();
        int amostraEventos;
        try {
            amostraEventos = linhaAmostra.isBlank() ? (modoExec == 2 ? 100_000 : 1_000_000)
                    : Integer.parseInt(linhaAmostra.trim());
        } catch (NumberFormatException e) {
            amostraEventos = modoExec == 2 ? 100_000 : 1_000_000;
        }
        if (amostraEventos <= 0) {
            amostraEventos = modoExec == 2 ? 100_000 : 1_000_000;
        }
        processador.imprimirCuriosidadesEventosAmostrais(amostraEventos, 10);
        System.out.println("\n" + "=".repeat(60));

        long tempoTotalMs = System.currentTimeMillis() - tempoInicioProcessamento;
        System.out.printf("%n>>> TEMPO TOTAL DO PROCESSAMENTO (alocação dos dados + benchmarks + relatórios + ranking): %s%n",
                formatarTempoDetalhado(tempoTotalMs));
        System.out.println("    (Se você demorou ao digitar a amostra de eventos, esse intervalo entra na conta.)");
    }
}