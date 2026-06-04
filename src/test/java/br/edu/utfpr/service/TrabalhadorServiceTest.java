package br.edu.utfpr.service;

import br.edu.utfpr.model.Trabalhador;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TrabalhadorServiceTest {

    private EntityManagerFactory emf;
    private EntityManager em;
    private TrabalhadorService trabalhadorService;

    @BeforeAll
    public void setUp() {
        emf = Persistence.createEntityManagerFactory("CanteiroTestPU");
    }

    @BeforeEach
    public void prepararTeste() {
        em = emf.createEntityManager();
        // Não iniciamos mais a transação aqui, pois o GenericDao já faz isso internamente
        trabalhadorService = new TrabalhadorService(em);
    }

    @AfterEach
    public void finalizarTeste() {
        // Apenas fechamos a conexão
        if (em != null && em.isOpen()) {
            em.close();
        }
    }

    @AfterAll
    public void tearDown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @Test
    public void testInserirEBuscarTrabalhador_DeveRetornarSucesso() {
        // Arrange
        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setNomeCompleto("João Silva");
        trabalhador.setCpf("11122233344");
        trabalhador.setFuncao("Pedreiro");
        trabalhador.setTipoContrato("CLT"); // Campo obrigatório no BD
        trabalhador.setDataContratacao(LocalDate.now()); // Campo obrigatório no BD

        // Act
        trabalhadorService.inserir(trabalhador);

        // Assert
        assertNotNull(trabalhador.getId(), "O ID não deve ser nulo após a inserção (JPA deve gerar o ID)");

        Trabalhador trabalhadorSalvo = trabalhadorService.buscarPorId(trabalhador.getId());
        assertEquals("João Silva", trabalhadorSalvo.getNomeCompleto());
        assertEquals("11122233344", trabalhadorSalvo.getCpf());
    }

    @Test
    public void testExisteCPF_QuandoExiste_DeveRetornarTrue() {
        // Arrange
        Trabalhador trabalhador = new Trabalhador();
        trabalhador.setNomeCompleto("Maria Souza");
        trabalhador.setCpf("99988877766");
        trabalhador.setFuncao("Encanador");
        trabalhador.setTipoContrato("PJ");
        trabalhador.setDataContratacao(LocalDate.now());
        trabalhadorService.inserir(trabalhador);

        // Act
        boolean existe = trabalhadorService.existeCPF("99988877766");

        // Assert
        assertTrue(existe, "O método deve retornar true pois o CPF acabou de ser salvo");
    }

    @Test
    public void testFiltrarPorFuncao_DeveRetornarListaFiltrada() {
        // Arrange
        Trabalhador t1 = new Trabalhador();
        t1.setNomeCompleto("Carlos Eletricista");
        t1.setCpf("00011122233");
        t1.setFuncao("Eletricista");
        t1.setTipoContrato("CLT");
        t1.setDataContratacao(LocalDate.now());
        trabalhadorService.inserir(t1);

        Trabalhador t2 = new Trabalhador();
        t2.setNomeCompleto("José Pedreiro");
        t2.setCpf("44455566677");
        t2.setFuncao("Pedreiro");
        t2.setTipoContrato("CLT");
        t2.setDataContratacao(LocalDate.now());
        trabalhadorService.inserir(t2);

        // Act
        List<Trabalhador> eletricistas = trabalhadorService.filtrarPorFuncao("Eletricista");

        // Assert
        assertTrue(eletricistas.stream().anyMatch(t -> t.getFuncao().equals("Eletricista")), "A lista deve conter um eletricista");
    }
}