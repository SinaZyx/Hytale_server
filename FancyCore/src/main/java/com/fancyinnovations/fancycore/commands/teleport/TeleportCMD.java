package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.main.FancyCorePlugin;
import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.commands.FancyLeafCommandBase;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

public class TeleportCMD extends FancyLeafCommandBase {

    private final RequiredArg<PlayerRef> targetArg;
    private final OptionalArg<PlayerRef> destArg;

    public TeleportCMD() {
        super("teleport", "Teleport players");
        addAliases("tp");

        this.targetArg = this.withRequiredArg("target", "Target player", ArgTypes.PLAYER_REF);
        this.destArg = this.withOptionalArg("destination", "Destination player", ArgTypes.PLAYER_REF);
    }

    @Override
    public void executeSync(@NotNull CommandContext ctx) {
        PlayerRef targetPlayerRef = targetArg.get(ctx);
        PlayerRef destPlayerRef = destArg.get(ctx);

        boolean twoArgs = (destPlayerRef != null);

        if (!twoArgs) {
            // Case 1: tp <target> (Self to Target)
            if (!ctx.isPlayer()) {
                sendMsg(ctx, "error.command.player_only");
                return;
            }

            if (targetPlayerRef == null) {
                sendMsg(ctx, "error.player.not_found");
                return;
            }

            Ref<EntityStore> targetRef = targetPlayerRef.getReference();
            if (targetRef == null || !targetRef.isValid()) {
                sendMsg(ctx, "teleport.error.target_not_in_world");
                return;
            }

            // Sender (Self)
            Ref<EntityStore> senderRef = ctx.senderAsPlayerRef();
            if (senderRef == null || !senderRef.isValid()) {
                sendMsg(ctx, "teleport.error.sender_not_in_world");
                return;
            }

            Store<EntityStore> targetStore = targetRef.getStore();
            World targetWorld = ((EntityStore) targetStore.getExternalData()).getWorld();

            targetWorld.execute(() -> {
                TransformComponent targetTransform = (TransformComponent) targetStore.getComponent(targetRef,
                        TransformComponent.getComponentType());
                HeadRotation targetRotation = (HeadRotation) targetStore.getComponent(targetRef,
                        HeadRotation.getComponentType());

                if (targetTransform == null)
                    return;

                Store<EntityStore> senderStore = senderRef.getStore();
                World senderWorld = ((EntityStore) senderStore.getExternalData()).getWorld();

                senderWorld.execute(() -> {
                    Teleport tp = new Teleport(targetWorld, targetTransform.getPosition().clone(),
                            (targetRotation != null) ? targetRotation.getRotation().clone() : null);
                    senderStore.addComponent(senderRef, Teleport.getComponentType(), tp);

                    sendMsg(ctx, "teleport.success.self");
                });
            });

        } else {
            // Case 2: tp <target> <destination> (Target to Destination)
            // Check permission? Assuming base perm covers it or added perm.
            // TeleportCMD usually requires "fancycore.commands.teleport".

            if (targetPlayerRef == null) {
                sendMsg(ctx, "error.player.not_found");
                return;
            }

            Ref<EntityStore> targetRef = targetPlayerRef.getReference();
            Ref<EntityStore> destRef = destPlayerRef.getReference();

            if (targetRef == null || !targetRef.isValid()) {
                sendMsg(ctx, "teleport.error.target_not_in_world");
                return;
            }
            if (destRef == null || !destRef.isValid()) {
                sendMsg(ctx, "teleport.error.destination_not_in_world");
                return;
            }

            Store<EntityStore> destStore = destRef.getStore();
            World destWorld = ((EntityStore) destStore.getExternalData()).getWorld();

            destWorld.execute(() -> {
                TransformComponent destTransform = (TransformComponent) destStore.getComponent(destRef,
                        TransformComponent.getComponentType());
                HeadRotation destRotation = (HeadRotation) destStore.getComponent(destRef,
                        HeadRotation.getComponentType());

                if (destTransform == null)
                    return;

                Store<EntityStore> targetStore = targetRef.getStore();
                World targetWorld = ((EntityStore) targetStore.getExternalData()).getWorld();

                targetWorld.execute(() -> {
                    Teleport tp = new Teleport(destWorld, destTransform.getPosition().clone(),
                            (destRotation != null) ? destRotation.getRotation().clone() : null);
                    targetStore.addComponent(targetRef, Teleport.getComponentType(), tp);

                    sendMsg(ctx, "teleport.success.others");
                });
            });
        }
    }

    private void sendMsg(CommandContext ctx, String key) {
        String lang = "en";
        try {
            if (ctx.isPlayer()) {
                FancyPlayer p = FancyCorePlugin.get().getPlayerService().getByUUID(ctx.sender().getUuid());
                if (p != null)
                    lang = p.getLanguage();
            }
        } catch (Exception ignored) {
        }
        FancyCorePlugin.get().getTranslationService().getMessage(key, lang).sendTo(ctx.sender());
    }
}
