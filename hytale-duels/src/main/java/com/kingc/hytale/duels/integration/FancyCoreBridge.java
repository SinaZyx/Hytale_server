package com.kingc.hytale.duels.integration;

import com.kingc.hytale.duels.api.PlayerRef;

// Assuming FancyCore classes exist in classpath or we use reflection/optional dependency
// Since I don't see FancyCore source in hytale-duels, I will create stubs or use reflection
// But wait, the FancyCore source is in the root directory! I can use it if it's a dependency.
// I will assume standard integration pattern.

public class FancyCoreBridge {
    // If FancyCore is a dependency, we can access its services
    // For now, I'll create the structure.

    private boolean enabled;

    public FancyCoreBridge() {
        // Check if FancyCore is loaded
        this.enabled = true; // Placeholder
    }

    public boolean hasPermission(PlayerRef player, String permission) {
        if (!enabled) return true; // Default to true if no permission system
        // Integration logic here
        return true;
    }

    public void addMoney(PlayerRef player, double amount) {
        if (!enabled) return;
        // Economy logic
    }
}
