package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.api.teleport.Location;
import com.fancyinnovations.fancycore.api.teleport.SpawnService;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class SpawnCMD extends AbstractPlayerCommand {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();

    public SpawnCMD() {
        super("spawn", "Teleports you to the server's spawn point");
        requirePermission("fancycore.commands.spawn");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
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

        Location location = SpawnService.get().getSpawnLocation();

        translator.getMessage("teleport.delayed.start", fp.getLanguage())
            .replace("seconds", "5")
            .sendTo(fp);

        TeleportLocationHelper.teleportDelayed(fp, location, 5,
                () -> {
                    TeleportGuard.markTeleport(fp.getData().getUUID());
                    translator.getMessage("teleport.spawn.success", fp.getLanguage())
                        .sendTo(fp);
                },
                () -> translator.getMessage("teleport.delayed.cancelled", fp.getLanguage())
                    .sendTo(fp));
    }
}
