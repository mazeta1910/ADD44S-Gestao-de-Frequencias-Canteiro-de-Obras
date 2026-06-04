package br.edu.utfpr.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ValidacaoUtilTest {

    // --- Testes para o método validarCPF ---

    @Test
    public void testValidarCpf_CpfValido_DeveRetornarTrue() {
        // Nota: Substitua por um CPF real e matematicamente válido para o teste passar.
        // Aqui usamos um exemplo gerado apenas para testes.
        String cpfValido = "11144477735";
        assertTrue(ValidacaoUtil.validarCPF(cpfValido), "Deveria aceitar um CPF válido");
    }

    @Test
    public void testValidarCpf_CpfComTodosDigitosIguais_DeveRetornarFalse() {
        assertFalse(ValidacaoUtil.validarCPF("11111111111"), "Não deve aceitar CPF com dígitos repetidos");
    }

    @Test
    public void testValidarCpf_CpfTamanhoInvalido_DeveRetornarFalse() {
        assertFalse(ValidacaoUtil.validarCPF("123"), "Não deve aceitar CPF com menos de 11 dígitos");
        assertFalse(ValidacaoUtil.validarCPF("1234567890123"), "Não deve aceitar CPF com mais de 11 dígitos");
        assertFalse(ValidacaoUtil.validarCPF(null), "Não deve aceitar CPF nulo");
    }

    // --- Testes para o método validarCREA ---

    @Test
    public void testValidarCREA_Valido_DeveRetornarTrue() {
        assertTrue(ValidacaoUtil.validarCREA("PR123456"), "Deveria aceitar um CREA no formato correto (2 letras e 6 a 10 números)");
    }

    @Test
    public void testValidarCREA_Invalido_DeveRetornarFalse() {
        assertFalse(ValidacaoUtil.validarCREA("P123"), "Faltam letras ou números no formato");
        assertFalse(ValidacaoUtil.validarCREA("PR1234A"), "Não deve aceitar letras no meio dos números");
        assertFalse(ValidacaoUtil.validarCREA(""), "Não deve aceitar CREA vazio");
    }

    // --- Testes para o método isFuncaoRequerCREA ---

    @Test
    public void testIsFuncaoRequerCREA_DeveRetornarTrue() {
        assertTrue(ValidacaoUtil.isFuncaoRequerCREA("Engenheiro Civil"), "Engenheiro requer CREA");
        assertTrue(ValidacaoUtil.isFuncaoRequerCREA("Técnico em Edificações"), "Técnico requer CREA");
    }

    @Test
    public void testIsFuncaoRequerCREA_DeveRetornarFalse() {
        assertFalse(ValidacaoUtil.isFuncaoRequerCREA("Pedreiro"), "Pedreiro não requer CREA");
        assertFalse(ValidacaoUtil.isFuncaoRequerCREA(null), "Entrada nula deve retornar falso");
    }

    // --- Testes para o método formatarCPF ---

    @Test
    public void testFormatarCPF_DeveFormatarCorretamente() {
        String cpfFormatado = ValidacaoUtil.formatarCPF("12345678901");
        assertEquals("123.456.789-01", cpfFormatado, "O CPF deve ser formatado com pontos e traço");
    }

    @Test
    public void testFormatarCPF_Invalido_DeveRetornarOriginal() {
        assertEquals("123", ValidacaoUtil.formatarCPF("123"), "CPFs com tamanho incorreto devem ser devolvidos sem formatação");
    }
}