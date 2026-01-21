package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerDeathPositionData;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerWorldData;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TeleportDeathBackCMD extends CommandBase {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();

    public TeleportDeathBackCMD() {
        super("teleportdeathback", "Teleports you to the location where you last died");
        addAliases("deathback", "deathtp", "deathteleport");
        requirePermission("fancycore.commands.teleportdeathback");
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            translator.getMessage("error.command.player_only").sendTo(ctx.sender());
            return;
        }

        FancyPlayer fp = FancyPlayerService.get().getByUUID(ctx.sender().getUuid());
        if (fp == null) {
            translator.getMessage("error.player.not_found").sendTo(ctx.sender());
            return;
        }

        String blockReason = TeleportGuard.checkSender(fp.getData().getUUID());
        if (blockReason != null) {
            fp.sendMessage(blockReason);
            return;
        }

        PlayerRef senderPlayerRef = fp.getPlayer();
        if (senderPlayerRef == null) {
            translator.getMessage("teleport.error.not_online", fp.getLanguage()).sendTo(fp);
            return;
        }

        Ref<EntityStore> senderRef = senderPlayerRef.getReference();
        if (senderRef == null || !senderRef.isValid()) {
            translator.getMessage("teleport.error.sender_not_in_world", fp.getLanguage()).sendTo(fp);
            return;
        }

        Store<EntityStore> senderStore = senderRef.getStore();
        World currentWorld = ((EntityStore) senderStore.getExternalData()).getWorld();

        // Get death position from PlayerWorldData
        currentWorld.execute(() -> {
            Player playerComponent = (Player) senderStore.getComponent(senderRef, Player.getComponentType());
            if (playerComponent == null) {
                translator.getMessage("teleport.death.failed", fp.getLanguage()).sendTo(fp);
                return;
            }

            PlayerWorldData perWorldData = playerComponent.getPlayerConfigData().getPerWorldData(currentWorld.getName());
            List<PlayerDeathPositionData> deathPositions = perWorldData.getDeathPositions();

            if (deathPositions == null || deathPositions.isEmpty()) {
                translator.getMessage("teleport.death.no_location", fp.getLanguage()).sendTo(fp);
                return;
            }

            // Get the most recent death (last in the list)
            PlayerDeathPositionData lastDeath = deathPositions.get(deathPositions.size() - 1);
            Transform deathTransform = lastDeath.getTransform();

            // Create Location from death position
            com.fancyinnovations.fancycore.api.teleport.Location deathLocation =
                new com.fancyinnovations.fancycore.api.teleport.Location(
                    currentWorld.getName(),
                    deathTransform.getPosition().getX(),
                    deathTransform.getPosition().getY(),
                    deathTransform.getPosition().getZ(),
                    deathTransform.getRotation().getYaw(),
                    deathTransform.getRotation().getPitch()
                );

            translator.getMessage("teleport.delayed.start", fp.getLanguage())
                .replace("seconds", "5")
                .sendTo(fp);

            TeleportLocationHelper.teleportDelayed(fp, deathLocation, 5,
                    () -> {
                        TeleportGuard.markTeleport(fp.getData().getUUID());
                        translator.getMessage("teleport.death.success", fp.getLanguage()).sendTo(fp);
                    },
                    () -> translator.getMessage("teleport.delayed.cancelled", fp.getLanguage()).sendTo(fp));
        });
    }
}
