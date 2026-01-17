package com.fancyinnovations.fancycore.utils;

import com.fancyinnovations.fancycore.api.FancyCore;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.oliver.fancyanalytics.logger.properties.ThrowableProperty;

public class MongoDBConnector {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private MongoClient client;
    private MongoDatabase database;


    public MongoDBConnector(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public boolean connect() {
        try {
            String connectionString;
            if (username.isEmpty() && password.isEmpty()) {
                connectionString = "mongodb://" + host + ":" + port;
            } else {
                connectionString = "mongodb://" + username + ":" + password + "@" + host + ":" + port;
            }

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(connectionString))
                    .build();

            client = MongoClients.create(settings);
            database = client.getDatabase("FancyAnalytics");
        } catch (Exception e) {
            FancyCore.get().getFancyLogger().warn("Failed to connect to MongoDB", ThrowableProperty.of(e));
            return false;
        }
        return true;
    }

    public void close() {
        client.close();
        client = null;
    }

    public boolean isConnected() {
        return client != null;
    }

    public MongoDatabase getDatabase() {
        return database;
    }
}
