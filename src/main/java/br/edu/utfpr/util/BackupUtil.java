package br.edu.utfpr.util;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BackupUtil {

    public static <T> void fazerBackup(List<T> dados, String caminho) throws IOException {
        Path path = Paths.get(caminho);
        Files.createDirectories(path.getParent());

        BackupData<T> backup = new BackupData<>(dados, LocalDateTime.now());

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(path.toFile()))) {
            oos.writeObject(backup);
        }

        System.out.println("💾 Backup salvo em: " + path.toAbsolutePath());
        System.out.println("📊 Registros: " + dados.size());
        System.out.println("🕐 Data/Hora: " + backup.getDataHora().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> restaurarBackup(String caminho) throws IOException, ClassNotFoundException {
        Path path = Paths.get(caminho);

        if (!Files.exists(path)) {
            throw new FileNotFoundException("Arquivo de backup não encontrado: " + caminho);
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(path.toFile()))) {
            BackupData<T> backup = (BackupData<T>) ois.readObject();

            System.out.println("💾 Backup restaurado de: " + path.toAbsolutePath());
            System.out.println("📊 Registros: " + backup.getDados().size());
            System.out.println("🕐 Data/Hora do backup: " + backup.getDataHora().format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

            return backup.getDados();
        }
    }

    private static class BackupData<T> implements Serializable {
        private static final long serialVersionUID = 1L;

        private final List<T> dados;
        private final LocalDateTime dataHora;

        public BackupData(List<T> dados, LocalDateTime dataHora) {
            this.dados = dados;
            this.dataHora = dataHora;
        }

        public List<T> getDados() {
            return dados;
        }

        public LocalDateTime getDataHora() {
            return dataHora;
        }
    }
}