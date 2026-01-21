package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.api.teleport.TeleportRequestService;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TeleportDenyCMD extends CommandBase {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();
    protected final OptionalArg<PlayerRef> senderArg = this.withOptionalArg("target", "The player who sent the request", ArgTypes.PLAYER_REF);

    public TeleportDenyCMD() {
        super("teleportdeny", "Denies a pending teleport request from another player");
        addAliases("tpd", "tpdeny");
        requirePermission("fancycore.commands.teleportdeny");
    }

    @Override
    protected void executeSync(@NotNull CommandContext ctx) {
        if (!ctx.isPlayer()) {
            translator.getMessage("error.command.player_only").sendTo(ctx.sender());
            return;
        }

        FancyPlayer target = FancyPlayerService.get().getByUUID(ctx.sender().getUuid());
        if (target == null) {
            translator.getMessage("error.player.not_found").sendTo(ctx.sender());
            return;
        }

        TeleportRequestService requestService = TeleportRequestService.get();
        UUID senderUUID;

        if (senderArg.provided(ctx)) {
            // Specific player provided
            PlayerRef senderPlayerRef = senderArg.get(ctx);
            FancyPlayer sender = FancyPlayerService.get().getByUUID(senderPlayerRef.getUuid());
            if (sender == null) {
                translator.getMessage("teleport.error.player_not_found", target.getLanguage()).sendTo(target);
                return;
            }

            senderUUID = requestService.getRequest(target, sender);
            if (senderUUID == null) {
                translator.getMessage("teleport.request.no_pending_from", target.getLanguage())
                    .replace("player", sender.getData().getUsername())
                    .sendTo(target);
                return;
            }
        } else {
            // No player specified, get first request
            senderUUID = requestService.getFirstRequest(target);
            if (senderUUID == null) {
                translator.getMessage("teleport.request.no_pending", target.getLanguage()).sendTo(target);
                return;
            }
        }

        FancyPlayer sender = FancyPlayerService.get().getByUUID(senderUUID);
        if (sender == null || !sender.isOnline()) {
            translator.getMessage("teleport.request.sender_offline", target.getLanguage()).sendTo(target);
            requestService.removeAllRequests(target);
            return;
        }

        // Remove the request
        if (requestService.removeRequest(target, sender)) {
            translator.getMessage("teleport.request.denied.self", target.getLanguage())
                .replace("player", sender.getData().getUsername())
                .sendTo(target);
            translator.getMessage("teleport.request.denied.other", sender.getLanguage())
                .replace("player", target.getData().getUsername())
                .sendTo(sender);
        } else {
            translator.getMessage("teleport.request.denied.failed", target.getLanguage()).sendTo(target);
        }
    }
}
