package com.kingc.hytale.nexus;

import com.google.gson.Gson;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.net.http.HttpClient;

public class NexusPlugin extends JavaPlugin {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private static final String BACKEND_URL = "http://localhost:3000/link/start";

    public NexusPlugin(JavaPluginInit init) {
        super(init);
    }

    // Removing @Override since onEnable might not be in the base class or named differently
    public void onEnable() {
        System.out.println("Nexus Plugin enabled!");
    }
}
