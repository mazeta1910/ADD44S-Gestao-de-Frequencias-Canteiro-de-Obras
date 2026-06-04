package br.edu.utfpr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.util.List;

public class ExportacaoService {

    public <T> void exportarParaTxt(List<T> dados, String caminhoCompleto, String titulo) throws IOException {
        File arquivo = new File(caminhoCompleto);
        arquivo.getParentFile().mkdirs();
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(arquivo))) {
            writer.write("=".repeat(50) + "\n");
            writer.write(titulo + "\n");
            writer.write("=".repeat(50) + "\n");
            writer.write("Total de registros: " + dados.size() + "\n");
            writer.write("Data da exportação: " + java.time.LocalDateTime.now() + "\n");
            writer.write("=".repeat(50) + "\n\n");
            
            for (T item : dados) {
                writer.write(item.toString() + "\n");
                writer.write("-".repeat(50) + "\n");
            }
        }
    }

    public <T> void exportarParaJson(List<T> dados, String caminhoCompleto) throws IOException {
        File arquivo = new File(caminhoCompleto);
        arquivo.getParentFile().mkdirs();
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        mapper.writeValue(arquivo, dados);
    }

    public <T> void exportarParaBin(List<T> dados, String caminhoCompleto) throws IOException {
        File arquivo = new File(caminhoCompleto);
        arquivo.getParentFile().mkdirs();
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(arquivo))) {
            oos.writeObject(dados);
        }
    }
    
    public String obterCaminhoAbsoluto(String caminhoRelativo) {
        return new File(caminhoRelativo).getAbsolutePath();
    }
}

