package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.fancyinnovations.fancycore.api.player.Home;
import com.fancyinnovations.fancycore.api.teleport.Location;
import com.fancyinnovations.fancycore.commands.teleport.TeleportGuard;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class HomeCMD extends AbstractPlayerCommand {

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
            ctx.sendMessage(Message.raw("This command can only be executed by a player."));
            return;
        }

        FancyPlayer fp = FancyPlayerService.get().getByUUID(ctx.sender().getUuid());
        if (fp == null) {
            ctx.sendMessage(Message.raw("FancyPlayer not found."));
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
                fp.sendMessage("Home with the name '" + nameArg.getName() + "' does not exist.");
                return;
            }
        } else {
            if (fp.getData().getHomes().isEmpty()) {
                fp.sendMessage("You do not have any homes set.");
                return;
            }
            home = fp.getData().getHomes().getFirst();
        }

        Location location = home.location();

        fp.sendMessage("Teleportation in 5 seconds...");

        com.fancyinnovations.fancycore.main.FancyCorePlugin.get().getThreadPool().schedule(() -> {
            PlayerRef currentPRef = fp.getPlayer();
            if (currentPRef == null || !currentPRef.isValid())
                return;
            Ref<EntityStore> currentRef = currentPRef.getReference();
            if (currentRef == null || !currentRef.isValid())
                return;
            Store<EntityStore> currentStore = currentRef.getStore();
            World targetWorld = Universe.get().getWorld(location.worldName());
            if (targetWorld == null)
                return;

            Teleport teleport = new Teleport(targetWorld, location.positionVec(), location.rotationVec());
            currentStore.addComponent(currentRef, Teleport.getComponentType(), teleport);
            TeleportGuard.markTeleport(fp.getData().getUUID());
            fp.sendMessage("Teleported to home.");
        }, 5, java.util.concurrent.TimeUnit.SECONDS);
    }
}
