package br.edu.utfpr;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MenuConsoleSimplificadoTest {

    private final InputStream tecladoOriginal = System.in;
    private final PrintStream ecraOriginal = System.out;
    private ByteArrayOutputStream ecraCapturado;

    @BeforeEach
    public void prepararTerminal() {
        ecraCapturado = new ByteArrayOutputStream();
        System.setOut(new PrintStream(ecraCapturado));
    }

    @AfterEach
    public void restaurarTerminal() {
        System.setIn(tecladoOriginal);
        System.setOut(ecraOriginal);
    }

    @Test
    public void testAberturaESaidaDoMenu_DeveMostrarMensagemDeDespedida() {
        // ARRANGE: Simulamos o utilizador a digitar "0" e a carregar no "Enter" (\n)
        String entradasSimuladas = "0\n";
        System.setIn(new ByteArrayInputStream(entradasSimuladas.getBytes()));

        // ACT: Iniciamos o menu principal da aplicação (removemos o try-catch para ver erros reais, se houverem)
        MenuConsoleSimplificado.main(new String[]{});

        // ASSERT: Lemos o que foi impresso
        String saidaDoMenu = ecraCapturado.toString();

        // Verificamos os textos EXACTOS que o seu sistema imprime (Case-Sensitive)
        assertTrue(saidaDoMenu.contains("SISTEMA DE GESTÃO"), "O cabeçalho do sistema não foi impresso.");
        assertTrue(saidaDoMenu.contains("MENU PRINCIPAL"), "O menu principal não foi impresso.");
        assertTrue(saidaDoMenu.contains("Encerrando sistema"), "A mensagem de despedida não foi impressa.");
    }
}