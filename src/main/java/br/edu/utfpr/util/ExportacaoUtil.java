package br.edu.utfpr.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportacaoUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static void exportarParaTxt(List<?> dados, String caminho, String tipo) throws IOException {
        Path path = Paths.get(caminho);
        Files.createDirectories(path.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write("=".repeat(80));
            writer.newLine();
            writer.write("EXPORTAÇÃO DE " + tipo.toUpperCase());
            writer.newLine();
            writer.write("Data/Hora: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            writer.newLine();
            writer.write("Total de registros: " + dados.size());
            writer.newLine();
            writer.write("=".repeat(80));
            writer.newLine();
            writer.newLine();

            for (Object obj : dados) {
                writer.write(obj.toString());
                writer.newLine();
                writer.write("-".repeat(80));
                writer.newLine();
            }
        }

        System.out.println("📄 Arquivo TXT salvo em: " + path.toAbsolutePath());
    }

    public static void exportarParaJson(List<?> dados, String caminho) throws IOException {
        Path path = Paths.get(caminho);
        Files.createDirectories(path.getParent());

        objectMapper.writeValue(path.toFile(), dados);

        System.out.println("📄 Arquivo JSON salvo em: " + path.toAbsolutePath());
    }

    public static void exportarParaBin(List<?> dados, String caminho) throws IOException {
        Path path = Paths.get(caminho);
        Files.createDirectories(path.getParent());

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(path.toFile()))) {
            oos.writeObject(dados);
        }

        System.out.println("📄 Arquivo BIN salvo em: " + path.toAbsolutePath());
    }

    public static <T> List<T> importarDeJson(String caminho, Class<T> tipo) throws IOException {
        Path path = Paths.get(caminho);
        return objectMapper.readValue(
                path.toFile(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, tipo)
        );
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> importarDeBin(String caminho) throws IOException, ClassNotFoundException {
        Path path = Paths.get(caminho);

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(path.toFile()))) {
            return (List<T>) ois.readObject();
        }
    }
}