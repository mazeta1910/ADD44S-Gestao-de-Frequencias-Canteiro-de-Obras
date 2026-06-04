package br.edu.utfpr.util;

public class ValidacaoUtil {

    public static boolean validarCPF(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return false;
        }
        
        if (cpf.matches("(\\d)\\1{10}")) {
            return false;
        }
        
        try {
            int[] digitos = new int[11];
            for (int i = 0; i < 11; i++) {
                digitos[i] = Integer.parseInt(cpf.substring(i, i + 1));
            }
            
            int soma = 0;
            for (int i = 0; i < 9; i++) {
                soma += digitos[i] * (10 - i);
            }
            int primeiroDigito = 11 - (soma % 11);
            if (primeiroDigito >= 10) primeiroDigito = 0;
            
            if (digitos[9] != primeiroDigito) {
                return false;
            }
            
            soma = 0;
            for (int i = 0; i < 10; i++) {
                soma += digitos[i] * (11 - i);
            }
            int segundoDigito = 11 - (soma % 11);
            if (segundoDigito >= 10) segundoDigito = 0;
            
            return digitos[10] == segundoDigito;
            
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean validarCREA(String crea) {
        if (crea == null || crea.trim().isEmpty()) {
            return false;
        }
        return crea.matches("^[A-Z]{2}\\d{6,10}$");
    }
    
    public static boolean isFuncaoRequerCREA(String funcao) {
        if (funcao == null) return false;
        String funcaoLower = funcao.toLowerCase();
        return funcaoLower.contains("engenheiro") || 
               funcaoLower.contains("arquiteto") ||
               funcaoLower.contains("técnico");
    }
    
    public static boolean isFuncaoRequerRegistro(String funcao) {
        if (funcao == null) return false;
        String funcaoLower = funcao.toLowerCase();
        return funcaoLower.contains("pedreiro") || 
               funcaoLower.contains("eletricista") ||
               funcaoLower.contains("encanador") ||
               funcaoLower.contains("carpinteiro") ||
               funcaoLower.contains("soldador");
    }
    
    public static String formatarCPF(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + 
               cpf.substring(3, 6) + "." + 
               cpf.substring(6, 9) + "-" + 
               cpf.substring(9, 11);
    }
}

