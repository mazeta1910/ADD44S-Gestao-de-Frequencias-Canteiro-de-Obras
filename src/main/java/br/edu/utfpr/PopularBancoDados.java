package br.edu.utfpr;

import br.edu.utfpr.model.*;
import br.edu.utfpr.service.*;
import br.edu.utfpr.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import java.time.LocalDate;

public class PopularBancoDados {

    private static void limparDadosExistentes(EntityManager em) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.createQuery("DELETE FROM RegistroPonto").executeUpdate();
            em.createQuery("DELETE FROM EPI").executeUpdate();
            em.createQuery("DELETE FROM Trabalhador").executeUpdate();
            em.createQuery("DELETE FROM Canteiro").executeUpdate();
            tx.commit();
            System.out.println("Dados anteriores removidos.\n");
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException("Erro ao limpar dados existentes: " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) {
        EntityManager em = JPAUtil.getEntityManager();

        TrabalhadorService trabalhadorService = new TrabalhadorService(em);
        CanteiroService canteiroService = new CanteiroService(em);
        EPIService epiService = new EPIService(em);

        System.out.println("Iniciando população do banco de dados...\n");
        limparDadosExistentes(em);

        Canteiro canteiro1 = new Canteiro();
        canteiro1.setNome("Obra Residencial Vila Nova");
        canteiro1.setLocalizacao("Rua das Flores, 123 - Curitiba/PR");
        canteiro1.setResponsavel("João Silva");
        canteiro1.setDataInicio(LocalDate.now().minusMonths(2));
        canteiro1.setDataPrevisaoTermino(LocalDate.now().plusMonths(10));
        canteiroService.inserir(canteiro1);
        System.out.println("✅ Canteiro 1 criado: " + canteiro1.getNome());

        Canteiro canteiro2 = new Canteiro();
        canteiro2.setNome("Construção Comercial Centro");
        canteiro2.setLocalizacao("Av. Principal, 456 - Curitiba/PR");
        canteiro2.setResponsavel("Maria Santos");
        canteiro2.setDataInicio(LocalDate.now().minusMonths(1));
        canteiro2.setDataPrevisaoTermino(LocalDate.now().plusMonths(8));
        canteiroService.inserir(canteiro2);
        System.out.println("✅ Canteiro 2 criado: " + canteiro2.getNome());

        Trabalhador eng1 = new Trabalhador();
        eng1.setNomeCompleto("Carlos Alberto Mendes");
        eng1.setCpf("12345678909");
        eng1.setFuncao("Engenheiro Civil");
        eng1.setDataContratacao(LocalDate.now().minusMonths(3));
        eng1.setTipoContrato("CLT");
        eng1.setNumeroCREA("123456");
        eng1.setEspecialidade("Estruturas");
        eng1.setCanteiroAtual(canteiro1);
        eng1.setAdministrador(false);
        trabalhadorService.inserir(eng1);
        System.out.println("✅ Trabalhador 1 criado: " + eng1.getNomeCompleto());

        Trabalhador ped1 = new Trabalhador();
        ped1.setNomeCompleto("José da Silva");
        ped1.setCpf("98765432100");
        ped1.setFuncao("Pedreiro");
        ped1.setDataContratacao(LocalDate.now().minusMonths(2));
        ped1.setTipoContrato("CLT");
        ped1.setNumeroRegistroProfissional("54321");
        ped1.setCanteiroAtual(canteiro1);
        ped1.setAdministrador(false);
        trabalhadorService.inserir(ped1);
        System.out.println("✅ Trabalhador 2 criado: " + ped1.getNomeCompleto());

        Trabalhador elet1 = new Trabalhador();
        elet1.setNomeCompleto("Paulo Roberto Lima");
        elet1.setCpf("11122233344");
        elet1.setFuncao("Eletricista");
        elet1.setDataContratacao(LocalDate.now().minusMonths(1));
        elet1.setTipoContrato("CLT");
        elet1.setNumeroRegistroProfissional("98765");
        elet1.setCanteiroAtual(canteiro2);
        elet1.setAdministrador(false);
        trabalhadorService.inserir(elet1);
        System.out.println("✅ Trabalhador 3 criado: " + elet1.getNomeCompleto());

        Trabalhador aux1 = new Trabalhador();
        aux1.setNomeCompleto("Antonio Costa");
        aux1.setCpf("55566677788");
        aux1.setFuncao("Auxiliar de Obras");
        aux1.setDataContratacao(LocalDate.now().minusWeeks(2));
        aux1.setTipoContrato("TEMPORARIO");
        aux1.setCanteiroAtual(canteiro1);
        aux1.setAdministrador(false);
        trabalhadorService.inserir(aux1);
        System.out.println("✅ Trabalhador 4 criado: " + aux1.getNomeCompleto());

        Trabalhador arq1 = new Trabalhador();
        arq1.setNomeCompleto("Ana Paula Ferreira");
        arq1.setCpf("22233344455");
        arq1.setFuncao("Arquiteto");
        arq1.setDataContratacao(LocalDate.now().minusMonths(4));
        arq1.setTipoContrato("PJ");
        arq1.setNumeroCREA("789012");
        arq1.setEspecialidade("Projetos Residenciais");
        arq1.setCanteiroAtual(canteiro2);
        arq1.setAdministrador(false);
        trabalhadorService.inserir(arq1);
        System.out.println("✅ Trabalhador 5 criado: " + arq1.getNomeCompleto());

        // --- INSERÇÃO DOS NOVOS PERFIS (Movido para antes do em.close()) ---

        // Criando o Engenheiro/Administrador (Acesso Total)
        Trabalhador admin = new Trabalhador();
        admin.setNomeCompleto("Matheus C. P. Santos");
        admin.setCpf("11111111111");
        admin.setFuncao("Engenheiro Chefe");
        admin.setAdministrador(true);
        admin.setDataContratacao(LocalDate.now());
        admin.setTipoContrato("CLT");
        trabalhadorService.inserir(admin);
        System.out.println("✅ Trabalhador Admin criado: " + admin.getNomeCompleto());

        // Criando Operário 1 (Acesso Restrito)
        Trabalhador operario1 = new Trabalhador();
        operario1.setNomeCompleto("João Mestre de Obras");
        operario1.setCpf("22222222222");
        operario1.setFuncao("Pedreiro");
        operario1.setAdministrador(false);
        operario1.setDataContratacao(LocalDate.now());
        operario1.setTipoContrato("CLT");
        trabalhadorService.inserir(operario1);
        System.out.println("✅ Trabalhador Operacional criado: " + operario1.getNomeCompleto());

        // Criando Operário 2 (Acesso Restrito)
        Trabalhador operario2 = new Trabalhador();
        operario2.setNomeCompleto("Maria Eletricista");
        operario2.setCpf("33333333333");
        operario2.setFuncao("Eletricista");
        operario2.setAdministrador(false);
        operario2.setDataContratacao(LocalDate.now());
        operario2.setTipoContrato("PJ");
        trabalhadorService.inserir(operario2);
        System.out.println("✅ Trabalhador Operacional criado: " + operario2.getNomeCompleto());

        Trabalhador estagiario1 = new Trabalhador();
        estagiario1.setNomeCompleto("Lucas Ferreira");
        estagiario1.setCpf("44444444444");
        estagiario1.setFuncao("Estagiario de Engenharia");
        estagiario1.setAdministrador(false);
        estagiario1.setDataContratacao(LocalDate.now());
        estagiario1.setTipoContrato("ESTAGIARIO");
        estagiario1.setCanteiroAtual(canteiro1);
        trabalhadorService.inserir(estagiario1);
        System.out.println("✅ Estagiario criado (meta 6h/dia): " + estagiario1.getNomeCompleto());

        // --- INSERÇÃO DOS EPIs ---

        EPI capacete1 = new EPI();
        capacete1.setTipo("Capacete");
        capacete1.setNumeroCA("12345");
        capacete1.setDataEntrega(LocalDate.now().minusMonths(2));
        capacete1.setDataValidade(LocalDate.now().plusYears(2));
        capacete1.setTrabalhador(eng1);
        epiService.inserir(capacete1);
        System.out.println("✅ EPI 1 criado: " + capacete1.getTipo());

        EPI bota1 = new EPI();
        bota1.setTipo("Bota de Segurança");
        bota1.setNumeroCA("54321");
        bota1.setDataEntrega(LocalDate.now().minusMonths(2));
        bota1.setDataValidade(LocalDate.now().plusYears(1));
        bota1.setTrabalhador(ped1);
        epiService.inserir(bota1);
        System.out.println("✅ EPI 2 criado: " + bota1.getTipo());

        EPI luva1 = new EPI();
        luva1.setTipo("Luva Isolante");
        luva1.setNumeroCA("98765");
        luva1.setDataEntrega(LocalDate.now().minusMonths(1));
        luva1.setDataValidade(LocalDate.now().plusMonths(6));
        luva1.setTrabalhador(elet1);
        epiService.inserir(luva1);
        System.out.println("✅ EPI 3 criado: " + luva1.getTipo());

        EPI oculos1 = new EPI();
        oculos1.setTipo("Óculos de Proteção");
        oculos1.setNumeroCA("11111");
        oculos1.setDataEntrega(LocalDate.now().minusWeeks(2));
        oculos1.setDataValidade(LocalDate.now().minusDays(10));
        oculos1.setTrabalhador(aux1);
        epiService.inserir(oculos1);
        System.out.println("✅ EPI 4 criado (VENCIDO): " + oculos1.getTipo());

        EPI capacete2 = new EPI();
        capacete2.setTipo("Capacete");
        capacete2.setNumeroCA("22222");
        capacete2.setDataEntrega(LocalDate.now().minusMonths(4));
        capacete2.setDataValidade(LocalDate.now().plusYears(3));
        capacete2.setTrabalhador(arq1);
        epiService.inserir(capacete2);
        System.out.println("✅ EPI 5 criado: " + capacete2.getTipo());

        // Fechando a conexão apenas no final de todas as operações
        em.close();

        System.out.println("\n" + "=".repeat(60));
        System.out.println("✅ BANCO DE DADOS POPULADO COM SUCESSO!");
        System.out.println("=".repeat(60));
        System.out.println("📊 Resumo:");
        System.out.println("   • 2 Canteiros");
        System.out.println("   • 9 Trabalhadores (inclui 1 estagiario com meta de 6h)");
        System.out.println("   • 5 EPIs (1 vencido para teste)");
        System.out.println("=".repeat(60));
    }
}