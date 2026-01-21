package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.api.teleport.TeleportRequestService;
import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.translations.TranslationService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class TeleportAcceptCMD extends CommandBase {

    private final TranslationService translator = FancyCorePlugin.get().getTranslationService();
    protected final OptionalArg<PlayerRef> senderArg = this.withOptionalArg("target", "The player who sent the request", ArgTypes.PLAYER_REF);

    public TeleportAcceptCMD() {
        super("teleportaccept", "Accepts a pending teleport request from another player");
        addAliases("tpa", "tpaccept");
         requirePermission("fancycore.commands.teleportaccept");
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

        String blockReason = TeleportGuard.checkSender(target.getData().getUUID());
        if (blockReason != null) {
            target.sendMessage(blockReason);
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

        String senderBlock = TeleportGuard.checkTarget(senderUUID);
        if (senderBlock != null) {
            translator.getMessage("teleport.request.refused", target.getLanguage())
                .replace("reason", senderBlock)
                .sendTo(target);
            translator.getMessage("teleport.request.refused", sender.getLanguage())
                .replace("reason", senderBlock)
                .sendTo(sender);
            return;
        }

        PlayerRef senderPlayerRef = sender.getPlayer();
        if (senderPlayerRef == null) {
            translator.getMessage("teleport.request.sender_offline", target.getLanguage()).sendTo(target);
            requestService.removeRequest(target, sender);
            return;
        }

        Ref<EntityStore> senderRef = senderPlayerRef.getReference();
        if (senderRef == null || !senderRef.isValid()) {
            translator.getMessage("teleport.request.sender_not_in_world", target.getLanguage()).sendTo(target);
            requestService.removeRequest(target, sender);
            return;
        }

        Ref<EntityStore> targetRef = ctx.senderAsPlayerRef();
        if (targetRef == null || !targetRef.isValid()) {
            translator.getMessage("teleport.error.sender_not_in_world", target.getLanguage()).sendTo(target);
            return;
        }

        Store<EntityStore> targetStore = targetRef.getStore();
        World targetWorld = ((EntityStore) targetStore.getExternalData()).getWorld();
        Store<EntityStore> senderStore = senderRef.getStore();
        World senderWorld = ((EntityStore) senderStore.getExternalData()).getWorld();

        // Remove the request
        requestService.removeRequest(target, sender);

        // Save sender's previous location for /back command (on sender's world thread)
        senderWorld.execute(() -> {
            TeleportLocationHelper.savePreviousLocation(sender, senderRef, senderStore, senderWorld);
        });

        // Get target's location on the target world thread
        targetWorld.execute(() -> {
            // Get target's transform and rotation
            TransformComponent targetTransformComponent = (TransformComponent) targetStore.getComponent(targetRef, TransformComponent.getComponentType());
            if (targetTransformComponent == null) {
                translator.getMessage("teleport.error.transform_failed", target.getLanguage()).sendTo(target);
                return;
            }

            HeadRotation targetHeadRotationComponent = (HeadRotation) targetStore.getComponent(targetRef, HeadRotation.getComponentType());
            if (targetHeadRotationComponent == null) {
                translator.getMessage("teleport.error.rotation_failed", target.getLanguage()).sendTo(target);
                return;
            }

            // Create Location from target position
            com.fancyinnovations.fancycore.api.teleport.Location targetLocation =
                new com.fancyinnovations.fancycore.api.teleport.Location(
                    targetWorld.getName(),
                    targetTransformComponent.getPosition().getX(),
                    targetTransformComponent.getPosition().getY(),
                    targetTransformComponent.getPosition().getZ(),
                    targetHeadRotationComponent.getRotation().getYaw(),
                    targetHeadRotationComponent.getRotation().getPitch()
                );

            // Send messages
            translator.getMessage("teleport.request.accepted.self", target.getLanguage())
                .replace("player", sender.getData().getUsername())
                .replace("seconds", "5")
                .sendTo(target);
            translator.getMessage("teleport.request.accepted.other", sender.getLanguage())
                .replace("player", target.getData().getUsername())
                .replace("seconds", "5")
                .sendTo(sender);

            // Use delayed teleportation
            TeleportLocationHelper.teleportDelayed(sender, targetLocation, 5,
                    () -> {
                        TeleportGuard.markTeleport(sender.getData().getUUID());
                        translator.getMessage("teleport.request.completed.sender", sender.getLanguage())
                            .replace("player", target.getData().getUsername())
                            .sendTo(sender);
                        translator.getMessage("teleport.request.completed.target", target.getLanguage())
                            .replace("player", sender.getData().getUsername())
                            .sendTo(target);
                    },
                    () -> {
                        translator.getMessage("teleport.request.cancelled.sender", sender.getLanguage()).sendTo(sender);
                        translator.getMessage("teleport.request.cancelled.target", target.getLanguage())
                            .replace("player", sender.getData().getUsername())
                            .sendTo(target);
                    });
        });
    }
}
