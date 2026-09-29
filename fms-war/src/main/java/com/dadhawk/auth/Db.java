package com.dadhawk.auth;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Db {
    private MongoClient client;

    public static String env(String k, String def) {
        String v = System.getenv(k);
        return v == null || v.isBlank() ? def : v;
    }

    @PostConstruct
    void init() {
        client = MongoClients.create(env("MONGO_URI", "mongodb://localhost:27017"));
    }

    @PreDestroy
    void close() {
        if (client != null) client.close();
    }

    public MongoDatabase db() {
        return client.getDatabase("dadhawk");
    }
}
