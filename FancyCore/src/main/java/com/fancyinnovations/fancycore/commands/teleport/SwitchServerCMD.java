package com.fancyinnovations.fancycore.commands.teleport;

import com.fancyinnovations.fancycore.api.player.FancyPlayer;
import com.fancyinnovations.fancycore.api.player.FancyPlayerService;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

public class SwitchServerCMD extends AbstractPlayerCommand {

    protected final RequiredArg<String> hostArg = this.withRequiredArg("host", "host ip or domain", ArgTypes.STRING);
    protected final OptionalArg<Integer> portArg = this.withOptionalArg("port", "port of server", ArgTypes.INTEGER);

    public SwitchServerCMD() {
        super("switchserver", "Switch to another server");
        requirePermission("fancycore.commands.switchserver");
    }

    @Override
    protected void execute(CommandContext ctx, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull PlayerRef playerRef, @NotNull World world) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command can only be executed by a player."));
            return;
        }

        FancyPlayer fp = FancyPlayerService.get().getByUUID(ctx.sender().getUuid());
        if (fp == null) {
            fp.sendMessage("FancyPlayer not found.");
            return;
        }

        String host = hostArg.get(ctx);
        Integer port;
        if (portArg.provided(ctx)) {
            port = portArg.get(ctx);
        } else {
            port = 5520;
        }

        playerRef.referToServer(host, port);
    }
}
