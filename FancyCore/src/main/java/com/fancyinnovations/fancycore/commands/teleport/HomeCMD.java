package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.api.player.Home;
import com.fancyinnovations.fancycore.api.teleport.Location;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class HomeCMD extends AbstractPlayerCommand {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();
    protected final OptionalArg<String> nameArg = this.withOptionalArg("home", "specific home name", ArgTypes.STRING);

    public HomeCMD() {
        super("home",
                "Teleports you to your home point with the specified name or the first home if no name is provided");
        addAliases("home", "h");
        requirePermission("fancycore.commands.home");
    }

    @Override
    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
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

        Home home;
        if (nameArg.provided(ctx)) {
            home = fp.getData().getHome(nameArg.getName());
            if (home == null) {
                translator.getMessage("teleport.home.not_found", fp.getLanguage())
                    .replace("name", nameArg.getName())
                    .sendTo(fp);
                return;
            }
        } else {
            if (fp.getData().getHomes().isEmpty()) {
                translator.getMessage("teleport.home.no_homes", fp.getLanguage())
                    .sendTo(fp);
                return;
            }
            home = fp.getData().getHomes().getFirst();
        }

        Location location = home.location();

        translator.getMessage("teleport.delayed.start", fp.getLanguage())
            .replace("seconds", "5")
            .sendTo(fp);

        TeleportLocationHelper.teleportDelayed(fp, location, 5,
                () -> {
                    TeleportGuard.markTeleport(fp.getData().getUUID());
                    translator.getMessage("teleport.home.success", fp.getLanguage())
                        .sendTo(fp);
                },
                () -> translator.getMessage("teleport.delayed.cancelled", fp.getLanguage())
                    .sendTo(fp));
    }
}
