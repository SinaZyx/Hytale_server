package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.api.teleport.TeleportRequestService;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;

public class TeleportRequestCMD extends CommandBase {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();
    protected final RequiredArg<PlayerRef> targetArg = this.withRequiredArg("target", "The player to request teleportation to", ArgTypes.PLAYER_REF);

    public TeleportRequestCMD() {
        super("teleportrequest", "Sends a teleport request to another player to teleport to their location");
        addAliases("tpr", "tprequest");
        requirePermission("fancycore.commands.teleportrequest");
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            translator.getMessage("error.command.player_only").sendTo(ctx.sender());
            return;
        }

        FancyPlayer sender = FancyPlayerService.get().getByUUID(ctx.sender().getUuid());
        if (sender == null) {
            translator.getMessage("error.player.not_found").sendTo(ctx.sender());
            return;
        }

        PlayerRef targetPlayerRef = targetArg.get(ctx);

        // Check if target player is online by checking their reference
        if (targetPlayerRef.getReference() == null || !targetPlayerRef.getReference().isValid()) {
            translator.getMessage("teleport.error.player_offline", sender.getLanguage()).sendTo(sender);
            return;
        }

        FancyPlayer target = FancyPlayerService.get().getByUUID(targetPlayerRef.getUuid());
        if (target == null) {
            translator.getMessage("teleport.error.player_not_found", sender.getLanguage()).sendTo(sender);
            return;
        }

        // Ensure the FancyPlayer has the PlayerRef set so sendMessage works
        if (target.getPlayer() == null) {
            target.setPlayer(targetPlayerRef);
        }

        if (sender.getData().getUUID().equals(target.getData().getUUID())) {
            translator.getMessage("teleport.request.no_self", sender.getLanguage()).sendTo(sender);
            return;
        }

        TeleportRequestService requestService = TeleportRequestService.get();
        if (requestService.sendRequest(sender, target)) {
            translator.getMessage("teleport.request.sent", sender.getLanguage())
                .replace("target", target.getData().getUsername())
                .sendTo(sender);
            translator.getMessage("teleport.request.received", target.getLanguage())
                .replace("player", sender.getData().getUsername())
                .sendTo(target);
        } else {
            translator.getMessage("teleport.request.already_pending", sender.getLanguage())
                .replace("player", target.getData().getUsername())
                .sendTo(sender);
        }
    }
}
