package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class TeleportBackCMD extends CommandBase {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();

    public TeleportBackCMD() {
        super("back", "Teleports you back to your previous location before your last teleport");
        requirePermission("fancycore.commands.back");
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
            translator.getMessage("teleport.error.not_online", fp.getLanguage())
                .sendTo(fp);
            return;
        }

        Ref<EntityStore> senderRef = senderPlayerRef.getReference();
        if (senderRef == null || !senderRef.isValid()) {
            translator.getMessage("teleport.error.sender_not_in_world", fp.getLanguage())
                .sendTo(fp);
            return;
        }

        // Get previous location from customData
        Map<String, Object> customData = fp.getData().getCustomData();
        Object backLocationObj = customData.get("teleport_back_location");
        if (backLocationObj == null || !(backLocationObj instanceof Map)) {
            translator.getMessage("teleport.back.no_location", fp.getLanguage())
                .sendTo(fp);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> backLocation = (Map<String, Object>) backLocationObj;
        String worldName = (String) backLocation.get("world");
        Double x = ((Number) backLocation.get("x")).doubleValue();
        Double y = ((Number) backLocation.get("y")).doubleValue();
        Double z = ((Number) backLocation.get("z")).doubleValue();
        Double yaw = ((Number) backLocation.get("yaw")).doubleValue();
        Double pitch = ((Number) backLocation.get("pitch")).doubleValue();

        // Create Location from stored data
        com.fancyinnovations.fancycore.api.teleport.Location targetLocation =
            new com.fancyinnovations.fancycore.api.teleport.Location(worldName, x, y, z, yaw.floatValue(), pitch.floatValue());

        translator.getMessage("teleport.delayed.start", fp.getLanguage())
            .replace("seconds", "5")
            .sendTo(fp);

        // Save current location before teleporting
        Store<EntityStore> senderStore = senderRef.getStore();
        World currentWorld = ((EntityStore) senderStore.getExternalData()).getWorld();
        TeleportLocationHelper.savePreviousLocation(fp, senderRef, senderStore, currentWorld);

        TeleportLocationHelper.teleportDelayed(fp, targetLocation, 5,
                () -> {
                    TeleportGuard.markTeleport(fp.getData().getUUID());
                    translator.getMessage("teleport.back.success", fp.getLanguage())
                        .sendTo(fp);
                },
                () -> translator.getMessage("teleport.delayed.cancelled", fp.getLanguage())
                    .sendTo(fp));
    }
}
