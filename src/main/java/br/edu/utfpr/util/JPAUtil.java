package br.edu.utfpr.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;

import java.util.HashMap;
import java.util.Map;

public class JPAUtil {

    private static final String PERSISTENCE_UNIT = "PostgresPU";
    private static final String NOME_BANCO = "trabalho_decente";
    private static final int PORTA_POSTGRES = 5432;

    private static EntityManagerFactory factory;
    private static String hostBanco;

    public static void configurarHostBanco(String host) {
        String hostNormalizado = (host == null || host.isBlank()) ? "localhost" : host.trim();

        synchronized (JPAUtil.class) {
            if (hostBanco != null && hostBanco.equals(hostNormalizado)
                    && factory != null && factory.isOpen()) {
                return;
            }

            fecharFactoryInterno();
            hostBanco = hostNormalizado;
            factory = criarFactory(hostNormalizado);
        }
    }

    private static EntityManagerFactory criarFactory(String host) {
        Map<String, Object> props = new HashMap<>();
        props.put(
                "jakarta.persistence.jdbc.url",
                "jdbc:postgresql://" + host + ":" + PORTA_POSTGRES + "/" + NOME_BANCO
        );

        try {
            return Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, props);
        } catch (PersistenceException e) {
            factory = null;
            throw new IllegalStateException(
                    "Falha ao conectar ao PostgreSQL em " + host + ":" + PORTA_POSTGRES
                            + "/" + NOME_BANCO + ". Verifique se o banco esta rodando.",
                    e
            );
        }
    }

    private static EntityManagerFactory getFactory() {
        synchronized (JPAUtil.class) {
            if (factory == null || !factory.isOpen()) {
                String host = hostBanco != null ? hostBanco : "localhost";
                factory = criarFactory(host);
            }
            return factory;
        }
    }

    public static EntityManager getEntityManager() {
        return getFactory().createEntityManager();
    }

    public static void close() {
        synchronized (JPAUtil.class) {
            fecharFactoryInterno();
        }
    }

    private static void fecharFactoryInterno() {
        if (factory != null && factory.isOpen()) {
            factory.close();
        }
        factory = null;
    }
}
