package com.example.app;

import com.example.app.menus.MainMenu;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws Exception {
        EntityManagerFactory emf = buildEntityManagerFactory();
        EntityManager em = emf.createEntityManager();
        try {
            new MainMenu(em).run();
        } finally {
            em.close();
            emf.close();
        }
    }

    private static EntityManagerFactory buildEntityManagerFactory() throws Exception {
        Properties props = new Properties();
        try (InputStream in = Main.class.getResourceAsStream("/config.properties")) {
            props.load(in);
        }
        Map<String, Object> overrides = new HashMap<>();
        for (String name : props.stringPropertyNames()) {
            overrides.put(name, props.getProperty(name));
        }
        return Persistence.createEntityManagerFactory("app-pu", overrides);
    }
}
